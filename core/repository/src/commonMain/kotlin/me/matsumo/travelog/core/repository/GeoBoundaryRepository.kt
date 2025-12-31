package me.matsumo.travelog.core.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
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
import me.matsumo.travelog.core.model.geo.haversineDistanceKm
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
        val searchQuery = query?.let { "$it $country" } ?: country

        val nominatimResult = nominatimDataSource.search(searchQuery)
        val overpassResult = overpassDataSource.getAdmins(nominatimResult.osmId, nominatimResult.placeRank)
        val elements = overpassResult.elements.sortedBy { it.tags.iso31662?.substringAfter("-") ?: "9999" }

        val countryIso2FromParam = country.takeIf { it.length == 2 }?.uppercase()
        val countryIso3FromParam = when {
            country.length == 3 -> country.uppercase()
            else -> countryIso2FromParam?.toIso3CountryCode()
        }

        val countryIso2 = countryIso2FromParam
            ?: nominatimResult.countryCode?.uppercase()
            ?: elements.firstNotNullOfOrNull { it.tags.iso31662 }?.substringBefore("-")
        val countryIso3 = countryIso3FromParam ?: countryIso2?.toIso3CountryCode()

        val targetAdminLevel = elements.firstNotNullOfOrNull { it.tags.adminLevel?.toIntOrNull() }
            ?: mapPlaceRankToAdminLevel(nominatimResult.placeRank)
        val geoBoundaryLevel = mapAdminLevelToGeoBoundaryLevel(targetAdminLevel)

        val geoJsonData = countryIso3?.let { iso3 ->
            try {
                getPolygon(iso3, geoBoundaryLevel)
            } catch (_: Exception) {
                null
            }
        }

        data class ParsedFeature(
            val polygons: List<PolygonWithHoles>,
            val bboxes: List<BoundingBox?>,
            val normalizedProperties: Set<String>,
        )

        val parsedFeatures = geoJsonData?.features?.mapNotNull { feature ->
            val parsed: List<PolygonWithHoles> = feature.geometry.toPolygons()
            if (parsed.isEmpty()) return@mapNotNull null

            val properties = feature.properties as? JsonObject
            ParsedFeature(
                polygons = parsed,
                bboxes = parsed.map { polygon -> polygon.boundingBox() },
                normalizedProperties = properties.extractNormalizedNames(),
            )
        } ?: emptyList()

        elements.map { element ->
            async {
                val nameCandidates = buildList {
                    add(element.tags.name)
                    element.tags.nameEn?.let { add(it) }
                    element.tags.nameJa?.let { add(it) }
                    element.tags.iso31662?.let { add(it) }
                }.map { it.normalizeName() }

                data class Candidate(
                    val polygon: PolygonWithHoles,
                    val priority: Int,
                    val distance: Double,
                )

                val candidates = parsedFeatures.flatMap { parsed ->
                    val featureNameMatch = parsed.normalizedProperties.any { it in nameCandidates }

                    parsed.polygons.mapIndexed { index, polygon ->
                        val bbox = parsed.bboxes.getOrNull(index)
                        val pip = isPointInPolygonWithHoles(element.center, polygon)
                        val bboxContains = bbox?.contains(element.center) == true
                        val distance = bbox?.let { haversineDistanceKm(it.center(), element.center) }
                            ?: Double.MAX_VALUE

                        val priority = when {
                            featureNameMatch && pip -> 0
                            featureNameMatch && bboxContains -> 1
                            pip -> 2
                            featureNameMatch -> 3
                            bboxContains -> 4
                            else -> 5
                        }

                        Candidate(
                            polygon = polygon,
                            priority = priority,
                            distance = distance,
                        )
                    }
                }

                val matchedPolygon: PolygonWithHoles = candidates.minWithOrNull(
                    compareBy<Candidate> { it.priority }
                        .thenBy { it.distance },
                )?.polygon ?: emptyList()

                val thumbnail = element.tags.wikipedia?.let { wiki ->
                    try {
                        getThumbnailUrl(wiki)
                    } catch (_: Exception) {
                        null
                    }
                }

                EnrichedRegion(
                    id = element.id,
                    tags = element.tags.asMap(),
                    center = element.center,
                    polygon = matchedPolygon,
                    thumbnailUrl = thumbnail,
                )
            }
        }.awaitAll()
    }

    suspend fun getThumbnailUrl(wikipedia: String): String? {
        val lang = wikipedia.substringBefore(':')
        val title = wikipedia.substringAfter(':')

        return wikipediaDataSource.getThumbnailUrl(lang, title)
    }

    private fun mapPlaceRankToAdminLevel(placeRank: Int): Int {
        return when {
            placeRank <= 4 -> 2   // Country level
            placeRank <= 12 -> 4  // State/Prefecture level
            placeRank <= 16 -> 6  // County/District level
            else -> 8             // City/Town level
        }
    }

    private fun mapAdminLevelToGeoBoundaryLevel(adminLevel: Int?): GeoBoundaryLevel {
        return when {
            adminLevel == null -> GeoBoundaryLevel.ADM1
            adminLevel <= 2 -> GeoBoundaryLevel.ADM0
            adminLevel in 3..4 -> GeoBoundaryLevel.ADM1
            adminLevel in 5..8 -> GeoBoundaryLevel.ADM2
            else -> GeoBoundaryLevel.ADM3
        }
    }

    private fun String.normalizeName(): String {
        val noPunct = lowercase().replace("[\\s\\p{Punct}]".toRegex(), "")

        return noPunct
            .replace("(都|道|府|県|州|省|市|区|町|村|郡)$".toRegex(), "")
            .replace("(city|ward|prefecture|pref|state|province|county|district|municipality)$".toRegex(), "")
    }

    private fun JsonObject?.extractNormalizedNames(): Set<String> {
        if (this == null) return emptySet()

        val names = mutableSetOf<String>()

        fun addPrimitive(key: String) {
            (this[key] as? JsonPrimitive)?.contentOrNull?.let { names.add(it.normalizeName()) }
        }

        addPrimitive("shapeName")
        addPrimitive("shapeISO")
        addPrimitive("shapeID")
        addPrimitive("shapeGroup")

        when (val alt = this["shapeNameAlt"]) {
            is JsonPrimitive -> alt.contentOrNull?.let { names.add(it.normalizeName()) }
            is JsonArray -> alt.forEach { element ->
                (element as? JsonPrimitive)?.contentOrNull?.let { names.add(it.normalizeName()) }
            }

            else -> {}
        }

        return names.filter { it.isNotEmpty() }.toSet()
    }

    private fun OverpassResult.Element.Tags.asMap(): Map<String, String> {
        return buildMap {
            put("name", name)
            iso31662?.let { put("ISO3166-2", it) }
            adminLevel?.let { put("admin_level", it) }
            boundary?.let { put("boundary", it) }
            nameEn?.let { put("name:en", it) }
            nameJa?.let { put("name:ja", it) }
            wikipedia?.let { put("wikipedia", it) }
        }
    }
}
