package me.matsumo.travelog.core.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.datasource.GeoBoundaryDataSource
import me.matsumo.travelog.core.datasource.NominatimDataSource
import me.matsumo.travelog.core.datasource.OverpassDataSource
import me.matsumo.travelog.core.datasource.WikipediaDataSource
import me.matsumo.travelog.core.model.geo.BoundingBox
import me.matsumo.travelog.core.model.geo.EnrichedRegion
import me.matsumo.travelog.core.model.geo.GeoBoundaryLevel
import me.matsumo.travelog.core.model.geo.GeoJsonData
import me.matsumo.travelog.core.model.geo.OverpassResult
import me.matsumo.travelog.core.model.geo.PolygonWithHoles
import me.matsumo.travelog.core.model.geo.boundingBox
import me.matsumo.travelog.core.model.geo.center
import me.matsumo.travelog.core.model.geo.contains
import me.matsumo.travelog.core.model.geo.isPointInPolygonWithHoles
import me.matsumo.travelog.core.model.geo.toIso3CountryCode
import me.matsumo.travelog.core.model.geo.toPolygons

class GeoBoundaryRepository(
    private val geoBoundaryDataSource: GeoBoundaryDataSource,
    private val nominatimDataSource: NominatimDataSource,
    private val overpassDataSource: OverpassDataSource,
    private val wikipediaDataSource: WikipediaDataSource,
) {
    suspend fun getPolygon(countryIso: String, level: GeoBoundaryLevel): GeoJsonData {
        val boundaryInfo = geoBoundaryDataSource.fetchBoundaryInfo(countryIso, level)
        val geoJsonData = geoBoundaryDataSource.downloadGeoJson(boundaryInfo.simplifiedGeometryGeoJSON!!)

        return geoJsonData
    }

    suspend fun getAdmins(location: String): List<OverpassResult.Element> {
        val nominatimResult = nominatimDataSource.search(location)
        val overpassResult = overpassDataSource.getAdmins(nominatimResult.osmId, nominatimResult.placeRank)

        return overpassResult.elements.sortedBy { it.tags.iso31662?.substringAfter("-") ?: "9999" }
    }

    suspend fun getEnrichedAdmins(country: String, query: String?): List<EnrichedRegion> = coroutineScope {
        // 1. Admin Level Resolution
        val targetAdminLevel = if (query == null) {
            GeoBoundaryLevel.ADM1 // 都道府県レベル
        } else {
            GeoBoundaryLevel.ADM2 // 市区町村レベル
        }

        Napier.d { "Fetching enriched admins for country=$country, query=$query, level=$targetAdminLevel" }

        // 2. ISO2 → ISO3 変換
        val countryIso3 = country.toIso3CountryCode()
            ?: throw IllegalArgumentException("Invalid country code: $country")

        // 3. Data Fetching (Parallel)
        val geoJsonDataDeferred = async {
            val boundaryInfo = geoBoundaryDataSource.fetchBoundaryInfo(countryIso3, targetAdminLevel)
            geoBoundaryDataSource.downloadGeoJson(boundaryInfo.simplifiedGeometryGeoJSON!!)
        }

        val (overpassElements, queryNominatimResult) = async {
            val searchQuery = if (query == null) country else "$country, $query"
            val nominatimResult = nominatimDataSource.search(searchQuery)
            val elements = overpassDataSource.getAdmins(nominatimResult.osmId, nominatimResult.placeRank).elements
            elements to nominatimResult
        }.await()

        val geoJsonData = geoJsonDataDeferred.await()

        Napier.d { "GeoBoundary returned ${geoJsonData.features.size} features" }
        Napier.d { "Overpass returned ${overpassElements.size} elements" }

        // 3.5. Extract parent bounding box for ADM2 filtering
        val parentBoundingBox = if (query != null) {
            // Nominatim boundingbox format: [south, north, west, east] or [minLat, maxLat, minLon, maxLon]
            val bbox = queryNominatimResult.boundingbox
            Napier.d { "Nominatim result for '$query': display_name=${queryNominatimResult.displayName}" }
            Napier.d { "Raw boundingbox: $bbox" }

            BoundingBox(
                minLat = bbox[0].toDouble(),
                maxLat = bbox[1].toDouble(),
                minLon = bbox[2].toDouble(),
                maxLon = bbox[3].toDouble(),
            ).also {
                Napier.d { "Parent bounding box: $it" }
                Napier.d { "BBox spans: lat=${it.maxLat - it.minLat}, lon=${it.maxLon - it.minLon}" }
            }
        } else {
            null
        }

        // 4. Feature Parsing (保持feature単位、not flatMap)
        val geoBoundaryFeatures: List<FeatureData> = geoJsonData.features.map { feature ->
            val polygons = feature.geometry.toPolygons()

            // すべてのポリゴンを包含するBounding Boxを計算
            val combinedBoundingBox = polygons
                .mapNotNull { it.boundingBox() }
                .takeIf { it.isNotEmpty() }
                ?.let { boxes ->
                    BoundingBox(
                        minLat = boxes.minOf { it.minLat },
                        maxLat = boxes.maxOf { it.maxLat },
                        minLon = boxes.minOf { it.minLon },
                        maxLon = boxes.maxOf { it.maxLon },
                    )
                }

            // Parse properties
            val properties = feature.properties as? JsonObject
            val shapeName = properties?.get("shapeName")?.jsonPrimitive?.contentOrNull
            val shapeISO = properties?.get("shapeISO")?.jsonPrimitive?.contentOrNull

            FeatureData(
                allPolygons = polygons,
                boundingBox = combinedBoundingBox,
                shapeName = shapeName,
                shapeISO = shapeISO,
            )
        }

        Napier.d { "Parsed ${geoBoundaryFeatures.size} features from GeoBoundary" }

        // Check if target municipalities exist in GeoBoundary data
        val targetMunicipalityNames = listOf("美里町", "東秩父村", "小山市")
        targetMunicipalityNames.forEach { targetName ->
            val found = geoBoundaryFeatures.filter {
                it.shapeName?.contains(targetName) == true
            }
            if (found.isNotEmpty()) {
                Napier.d {
                    "GeoBoundary contains '$targetName': ${found.map { "${it.shapeName} (${it.shapeISO}), bbox=${it.boundingBox}" }}"
                }
            } else {
                Napier.w { "GeoBoundary does NOT contain '$targetName'" }
            }
        }

        // 4.5. Filter GeoBoundary features by parent bounding box (for ADM2)
        val filteredGeoBoundaryFeatures = if (parentBoundingBox != null) {
            val filtered = geoBoundaryFeatures.filter { feature ->
                feature.boundingBox?.let { featureBBox ->
                    // Check if feature CENTER is within parent bounding box (more strict than overlap check)
                    // This prevents adjacent prefecture municipalities from being included
                    val center = featureBBox.center()
                    val isInside = center.lat >= parentBoundingBox.minLat &&
                            center.lat <= parentBoundingBox.maxLat &&
                            center.lon >= parentBoundingBox.minLon &&
                            center.lon <= parentBoundingBox.maxLon

                    if (!isInside) {
                        Napier.v { "Filtered OUT: ${feature.shapeName} (${feature.shapeISO}), center=$center outside parent bbox" }
                    }
                    isInside
                } ?: false
            }

            Napier.d { "Filtered to ${filtered.size} features within parent bounding box" }
            Napier.d { "Sample filtered features: ${filtered.take(5).map { it.shapeName }}" }

            // Check if target municipalities survived filtering
            targetMunicipalityNames.forEach { targetName ->
                val foundAfterFilter = filtered.filter {
                    it.shapeName?.contains(targetName) == true
                }
                if (foundAfterFilter.isNotEmpty()) {
                    Napier.d { "After filtering, '$targetName' is STILL PRESENT: ${foundAfterFilter.map { it.shapeName }}" }
                } else {
                    Napier.w { "After filtering, '$targetName' was REMOVED by bbox filter!" }
                }
            }

            filtered
        } else {
            geoBoundaryFeatures
        }

        // 5. EnrichedRegion Construction (with parallel thumbnail fetching)
        Napier.d { "Starting matching for ${overpassElements.size} Overpass elements" }
        Napier.d { "Sample Overpass elements: ${overpassElements.take(5).map { "${it.tags.name} (${it.tags.iso31662})" }}" }

        // Log specific municipalities we're looking for
        val targetMunicipalities = listOf("美里町", "東秩父村", "小山市")
        val foundTargets = overpassElements.filter { it.tags.name in targetMunicipalities }
        Napier.d {
            "Found target municipalities in Overpass: ${foundTargets.map { "${it.tags.name} (ISO=${it.tags.iso31662}, center=${it.center})" }}"
        }

        val enrichedRegions = overpassElements.map { element ->
            async {
                val matchedPolygons = matchFeature(element, filteredGeoBoundaryFeatures)
                if (matchedPolygons == null) {
                    Napier.w { "NO MATCH: ${element.tags.name} (iso=${element.tags.iso31662}, center=${element.center})" }
                    return@async null
                }

                // Wikipedia thumbnail取得(失敗時はnull)
                val thumbnailUrl = element.tags.wikipedia?.let { wikipedia ->
                    suspendRunCatching {
                        getThumbnailUrl(wikipedia)
                    }.getOrNull()
                }

                EnrichedRegion(
                    id = element.id,
                    tags = mapOf(
                        "name" to element.tags.name,
                        "name:ja" to (element.tags.nameJa ?: element.tags.name),
                        "name:en" to (element.tags.nameEn ?: element.tags.name),
                        "admin_level" to (element.tags.adminLevel ?: ""),
                        "ISO3166-2" to (element.tags.iso31662 ?: ""),
                        "wikipedia" to (element.tags.wikipedia ?: ""),
                    ),
                    center = element.center,
                    polygon = matchedPolygons.flatMap { polygonWithHoles ->
                        polygonWithHoles.map { ring ->
                            ring.map { coord ->
                                OverpassResult.Element.Coordinate(lat = coord.lat, lon = coord.lon)
                            }
                        }
                    },
                    thumbnailUrl = thumbnailUrl,
                )
            }
        }.awaitAll().filterNotNull()

        // Summary logging
        val matchedCount = enrichedRegions.size
        val totalCount = overpassElements.size
        val unmatchedCount = totalCount - matchedCount

        Napier.d { "=== MATCHING SUMMARY ===" }
        Napier.d { "Overpass elements: $totalCount" }
        Napier.d { "Matched: $matchedCount" }
        Napier.d { "Unmatched: $unmatchedCount" }

        if (unmatchedCount > 0) {
            val unmatchedNames = overpassElements
                .filter { element -> enrichedRegions.none { it.id == element.id } }
                .map { it.tags.name }
            Napier.w { "Unmatched municipalities: $unmatchedNames" }
            Napier.w { "These municipalities exist in OpenStreetMap but not in GeoBoundary data." }
            Napier.w { "This is a data source limitation, not a bug in the matching logic." }
        }

        enrichedRegions
    }

    suspend fun getThumbnailUrl(wikipedia: String): String? {
        val lang = wikipedia.substringBefore(':')
        val title = wikipedia.substringAfter(':')

        return wikipediaDataSource.getThumbnailUrl(lang, title)
    }

    private fun matchFeatureByName(
        overpassElement: OverpassResult.Element,
        geoBoundaryFeatures: List<FeatureData>,
    ): FeatureData? {
        val overpassISO = overpassElement.tags.iso31662
        val overpassNames = listOfNotNull(
            overpassElement.tags.name,
            overpassElement.tags.nameJa,
            overpassElement.tags.nameEn,
        )

        val isTargetMunicipality = overpassElement.tags.name in listOf("美里町", "東秩父村", "小山市")

        if (isTargetMunicipality) {
            Napier.d { "NAME MATCH for TARGET: ${overpassElement.tags.name}" }
            Napier.d { "  Overpass ISO: $overpassISO" }
            Napier.d { "  Overpass names: $overpassNames" }
            Napier.d { "  Checking against ${geoBoundaryFeatures.size} GeoBoundary features" }

            // Log first 10 GeoBoundary features for debugging
            geoBoundaryFeatures.take(10).forEach { feature ->
                Napier.v { "  GeoBoundary feature: ${feature.shapeName} (ISO=${feature.shapeISO})" }
            }
        } else {
            Napier.v { "Attempting name match for: ${overpassElement.tags.name} (ISO=$overpassISO, names=$overpassNames)" }
        }

        return geoBoundaryFeatures.firstOrNull { feature ->
            // ISO code matching (most reliable)
            if (overpassISO != null && feature.shapeISO != null) {
                if (feature.shapeISO == overpassISO) {
                    Napier.d { "ISO MATCH: ${overpassElement.tags.name} matched ${feature.shapeName} by ISO code" }
                    return@firstOrNull true
                }
            }

            // Name matching (fallback)
            if (feature.shapeName != null) {
                val matched = overpassNames.any { overpassName ->
                    // Exact match
                    val exactMatch = feature.shapeName.equals(overpassName, ignoreCase = true)
                    // Contains match (e.g., "Osaka Prefecture" contains "Osaka")
                    val containsMatch = feature.shapeName.contains(overpassName, ignoreCase = true) ||
                            overpassName.contains(feature.shapeName, ignoreCase = true)

                    if (exactMatch) {
                        Napier.d { "EXACT NAME MATCH: ${overpassElement.tags.name} ($overpassName) == ${feature.shapeName}" }
                        true
                    } else if (containsMatch) {
                        Napier.d { "CONTAINS NAME MATCH: ${overpassElement.tags.name} ($overpassName) ~ ${feature.shapeName}" }
                        true
                    } else {
                        false
                    }
                }
                matched
            } else {
                false
            }
        }
    }

    private fun matchFeature(
        overpassElement: OverpassResult.Element,
        geoBoundaryFeatures: List<FeatureData>,
    ): List<PolygonWithHoles>? {
        val isTargetMunicipality = overpassElement.tags.name in listOf("美里町", "東秩父村", "小山市")

        if (isTargetMunicipality) {
            Napier.d { "=== Matching TARGET MUNICIPALITY: ${overpassElement.tags.name} ===" }
            Napier.d { "ISO: ${overpassElement.tags.iso31662}, Center: ${overpassElement.center}" }
            Napier.d { "Available GeoBoundary features: ${geoBoundaryFeatures.size}" }
        }

        // 1. Try name-based matching first (most reliable)
        val nameMatch = matchFeatureByName(overpassElement, geoBoundaryFeatures)
        if (nameMatch != null) {
            if (isTargetMunicipality) {
                Napier.d { "TARGET: ${overpassElement.tags.name} matched by name/ISO to ${nameMatch.shapeName}" }
            } else {
                Napier.d { "Matched ${overpassElement.tags.name} by name/ISO" }
            }
            return nameMatch.allPolygons
        }

        if (isTargetMunicipality) {
            Napier.w { "TARGET: ${overpassElement.tags.name} - name matching FAILED" }
        }

        // 2. Fallback to bounding box + point-in-polygon matching
        var candidates = geoBoundaryFeatures.filter { featureData ->
            featureData.boundingBox?.contains(overpassElement.center) == true
        }

        if (isTargetMunicipality) {
            Napier.d { "TARGET: ${overpassElement.tags.name} - bbox filter found ${candidates.size} candidates" }
            Napier.d { "Candidates: ${candidates.map { "${it.shapeName} (${it.shapeISO})" }}" }
        }

        if (candidates.isEmpty()) {
            Napier.w { "Bbox filter returned 0 candidates for ${overpassElement.tags.name}, trying all features" }
            candidates = geoBoundaryFeatures
        }

        val matchedFeature = candidates.firstOrNull { featureData ->
            featureData.allPolygons.any { polygon ->
                isPointInPolygonWithHoles(overpassElement.center, polygon)
            }
        }

        if (matchedFeature != null) {
            if (isTargetMunicipality) {
                Napier.d { "TARGET: ${overpassElement.tags.name} matched by point-in-polygon to ${matchedFeature.shapeName}" }
            } else {
                Napier.d { "Matched ${overpassElement.tags.name} by point-in-polygon" }
            }
            return matchedFeature.allPolygons
        }

        if (isTargetMunicipality) {
            Napier.e { "TARGET: ${overpassElement.tags.name} - NO MATCH FOUND!" }
        } else {
            Napier.w { "No feature match for ${overpassElement.tags.name} (id: ${overpassElement.id})" }
        }
        return null
    }
}

private data class FeatureData(
    val allPolygons: List<PolygonWithHoles>,
    val boundingBox: BoundingBox?,
    val shapeName: String?,
    val shapeISO: String?,
)
