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
import me.matsumo.travelog.core.model.geo.NominatimResult
import me.matsumo.travelog.core.model.geo.OverpassResult
import me.matsumo.travelog.core.model.geo.PolygonWithHoles
import me.matsumo.travelog.core.model.geo.boundingBox
import me.matsumo.travelog.core.model.geo.center
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
        val targetAdminLevel = if (query == null) GeoBoundaryLevel.ADM1 else GeoBoundaryLevel.ADM2
        val countryIso3 = country.toIso3CountryCode()
            ?: throw IllegalArgumentException("Invalid country code: $country")

        Napier.d { "Fetching enriched admins: country=$country, query=$query, level=$targetAdminLevel" }

        // Parallel data fetching
        val geoJsonDataDeferred = async {
            val boundaryInfo = geoBoundaryDataSource.fetchBoundaryInfo(countryIso3, targetAdminLevel)
            geoBoundaryDataSource.downloadGeoJson(boundaryInfo.simplifiedGeometryGeoJSON!!)
        }

        val searchQuery = if (query == null) country else "$country, $query"
        val queryDeferred = async {
            val nominatim = nominatimDataSource.search(searchQuery)
            val overpass = overpassDataSource.getAdmins(nominatim.osmId, nominatim.placeRank)
            nominatim to overpass.elements
        }

        val geoJsonData = geoJsonDataDeferred.await()
        val (nominatimResult, overpassElements) = queryDeferred.await()

        Napier.d { "GeoBoundary: ${geoJsonData.features.size} features, Overpass: ${overpassElements.size} elements" }

        // Filter features for ADM2
        val filteredFeatures = if (query != null) {
            filterAdm2ByPrefecture(countryIso3, nominatimResult, geoJsonData)
        } else {
            geoJsonData.features.map { parseFeature(it) }
        }

        Napier.d { "Filtered features: ${filteredFeatures.size}" }

        // Match and build enriched regions
        buildEnrichedRegions(overpassElements, filteredFeatures)
    }

    private suspend fun filterAdm2ByPrefecture(
        countryIso3: String,
        prefectureNominatim: NominatimResult,
        adm2GeoJson: GeoJsonData,
    ): List<FeatureData> = coroutineScope {
        Napier.d { "Filtering ADM2 by prefecture: ${prefectureNominatim.name}" }

        // Fetch ADM1 (prefecture-level) GeoJSON
        val adm1Info = geoBoundaryDataSource.fetchBoundaryInfo(countryIso3, GeoBoundaryLevel.ADM1)
        val adm1GeoJson = geoBoundaryDataSource.downloadGeoJson(adm1Info.simplifiedGeometryGeoJSON!!)

        Napier.d { "ADM1 features: ${adm1GeoJson.features.size}" }

        // Find prefecture polygon by Nominatim center coordinate
        val prefecturePolygons = adm1GeoJson.features
            .firstOrNull { feature ->
                val polygons = feature.geometry.toPolygons()
                polygons.any { polygon ->
                    isPointInPolygonWithHoles(prefectureNominatim.center, polygon)
                }
            }?.geometry?.toPolygons()

        if (prefecturePolygons == null) {
            Napier.w { "Prefecture polygon not found for ${prefectureNominatim.name}" }
            return@coroutineScope emptyList()
        }

        Napier.d { "Found prefecture polygon with ${prefecturePolygons.size} polygon(s)" }

        // Filter ADM2 features by prefecture polygon
        val filtered = adm2GeoJson.features.mapNotNull { feature ->
            val featureData = parseFeature(feature)
            val center = featureData.boundingBox?.center() ?: return@mapNotNull null

            // Check if center is inside prefecture polygon
            val isInside = prefecturePolygons.any { polygon ->
                isPointInPolygonWithHoles(center, polygon)
            }

            if (isInside) featureData else null
        }

        Napier.d { "ADM2 filtered: ${adm2GeoJson.features.size} -> ${filtered.size}" }

        filtered
    }

    private fun parseFeature(feature: me.matsumo.travelog.core.model.geo.GeoJsonFeature): FeatureData {
        val polygons = feature.geometry.toPolygons()
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

        val properties = feature.properties as? JsonObject
        val shapeName = properties?.get("shapeName")?.jsonPrimitive?.contentOrNull
        val shapeISO = properties?.get("shapeISO")?.jsonPrimitive?.contentOrNull

        return FeatureData(
            allPolygons = polygons,
            boundingBox = combinedBoundingBox,
            shapeName = shapeName,
            shapeISO = shapeISO,
        )
    }

    private suspend fun buildEnrichedRegions(
        overpassElements: List<OverpassResult.Element>,
        features: List<FeatureData>,
    ): List<EnrichedRegion> = coroutineScope {
        val enrichedRegions = overpassElements.map { element ->
            async {
                val matchedPolygons = matchFeature(element, features)
                if (matchedPolygons == null) {
                    Napier.w { "No match: ${element.tags.name}" }
                    return@async null
                }

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

        Napier.d { "Matched: ${enrichedRegions.size}/${overpassElements.size}" }

        enrichedRegions
    }

    suspend fun getThumbnailUrl(wikipedia: String): String? {
        val lang = wikipedia.substringBefore(':')
        val title = wikipedia.substringAfter(':')

        if (lang.isEmpty() || title.isEmpty()) {
            return null
        }

        return wikipediaDataSource.getThumbnailUrl(lang, title)
    }

    private fun matchFeature(
        overpassElement: OverpassResult.Element,
        geoBoundaryFeatures: List<FeatureData>,
    ): List<PolygonWithHoles>? {
        // Try name-based matching first
        val nameMatch = matchFeatureByName(overpassElement, geoBoundaryFeatures)
        if (nameMatch != null) {
            return nameMatch.allPolygons
        }

        // Fallback to point-in-polygon
        val matchedFeature = geoBoundaryFeatures.firstOrNull { featureData ->
            featureData.allPolygons.any { polygon ->
                isPointInPolygonWithHoles(overpassElement.center, polygon)
            }
        }

        return matchedFeature?.allPolygons
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

        return geoBoundaryFeatures.firstOrNull { feature ->
            // ISO code matching (most reliable)
            if (overpassISO != null && feature.shapeISO != null) {
                if (feature.shapeISO == overpassISO) {
                    return@firstOrNull true
                }
            }

            // Name matching (fallback)
            if (feature.shapeName != null) {
                overpassNames.any { overpassName ->
                    feature.shapeName.equals(overpassName, ignoreCase = true) ||
                            feature.shapeName.contains(overpassName, ignoreCase = true) ||
                            overpassName.contains(feature.shapeName, ignoreCase = true)
                }
            } else {
                false
            }
        }
    }
}

private data class FeatureData(
    val allPolygons: List<PolygonWithHoles>,
    val boundingBox: BoundingBox?,
    val shapeName: String?,
    val shapeISO: String?,
)
