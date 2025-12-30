package me.matsumo.travelog.core.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import me.matsumo.travelog.core.datasource.GeoBoundaryDataSource
import me.matsumo.travelog.core.datasource.NominatimDataSource
import me.matsumo.travelog.core.datasource.OverpassDataSource
import me.matsumo.travelog.core.datasource.WikipediaDataSource
import me.matsumo.travelog.core.model.geo.EnrichedRegion
import me.matsumo.travelog.core.model.geo.GeoBoundaryLevel
import me.matsumo.travelog.core.model.geo.GeoJsonData
import me.matsumo.travelog.core.model.geo.OverpassResult
import me.matsumo.travelog.core.model.geo.PolygonWithHoles
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
        val targetAdminLevel = mapPlaceRankToAdminLevel(nominatimResult.placeRank)
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
            val properties: JsonObject?,
        )

        val parsedFeatures = geoJsonData?.features?.mapNotNull { feature ->
            val parsed: List<PolygonWithHoles> = feature.geometry.toPolygons()
            if (parsed.isEmpty()) return@mapNotNull null

            ParsedFeature(
                polygons = parsed,
                properties = feature.properties as? JsonObject,
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

                val matchedPolygon: PolygonWithHoles = parsedFeatures.firstNotNullOfOrNull { parsed ->
                    parsed.polygons.firstOrNull { polygon ->
                        isPointInPolygonWithHoles(element.center, polygon)
                    }
                } ?: parsedFeatures.firstNotNullOfOrNull { parsed ->
                    val props = parsed.properties
                    val propNames = buildList {
                        props?.get("shapeName")?.jsonPrimitive?.contentOrNull?.let { name ->
                            add(name.normalizeName())
                        }
                        props?.get("shapeISO")?.jsonPrimitive?.contentOrNull?.let { iso ->
                            add(iso.normalizeName())
                        }
                        props?.get("shapeID")?.jsonPrimitive?.contentOrNull?.let { id ->
                            add(id.normalizeName())
                        }
                    }

                    if (propNames.any { candidate -> candidate in nameCandidates }) {
                        parsed.polygons.firstOrNull()
                    } else {
                        null
                    }
                } ?: emptyList()

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
        return lowercase().replace("[\\s\\p{Punct}]".toRegex(), "")
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
