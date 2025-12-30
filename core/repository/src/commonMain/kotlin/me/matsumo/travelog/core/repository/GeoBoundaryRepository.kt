package me.matsumo.travelog.core.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.matsumo.travelog.core.datasource.GeoBoundaryDataSource
import me.matsumo.travelog.core.datasource.NominatimDataSource
import me.matsumo.travelog.core.datasource.OverpassDataSource
import me.matsumo.travelog.core.datasource.WikipediaDataSource
import me.matsumo.travelog.core.model.geo.EnrichedRegion
import me.matsumo.travelog.core.model.geo.GeoBoundaryLevel
import me.matsumo.travelog.core.model.geo.GeoJsonData
import me.matsumo.travelog.core.model.geo.OverpassResult
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

    suspend fun getEnrichedAdmins(location: String): List<EnrichedRegion> = coroutineScope {
        val nominatimResult = nominatimDataSource.search(location)
        val overpassResult = overpassDataSource.getAdmins(nominatimResult.osmId, nominatimResult.placeRank)
        val elements = overpassResult.elements.sortedBy { it.tags.iso31662?.substringAfter("-") ?: "9999" }

        val countryIso2 = elements.firstNotNullOfOrNull { it.tags.iso31662 }?.substringBefore("-")
        val countryIso3 = countryIso2?.toIso3CountryCode()
            ?: nominatimResult.countryCode?.toIso3CountryCode()
        val adminLevel = elements.firstNotNullOfOrNull { it.tags.adminLevel?.toIntOrNull() }
        val geoBoundaryLevel = mapAdminLevelToGeoBoundaryLevel(adminLevel)

        val geoJsonData = countryIso3?.let { iso3 ->
            try {
                getPolygon(iso3, geoBoundaryLevel)
            } catch (_: Exception) {
                null
            }
        }

        val polygons = geoJsonData?.features?.mapNotNull { feature ->
            val parsed = feature.geometry.toPolygons()
            parsed.ifEmpty { null }
        } ?: emptyList()

        elements.map { element ->
            async {
                val matchedPolygon = polygons.firstNotNullOfOrNull { polygonGroup ->
                    polygonGroup.firstOrNull { polygon ->
                        isPointInPolygonWithHoles(element.center, polygon)
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

    private fun mapAdminLevelToGeoBoundaryLevel(adminLevel: Int?): GeoBoundaryLevel {
        return when {
            adminLevel == null -> GeoBoundaryLevel.ADM1
            adminLevel <= 2 -> GeoBoundaryLevel.ADM0
            adminLevel in 3..4 -> GeoBoundaryLevel.ADM1
            adminLevel in 5..6 -> GeoBoundaryLevel.ADM2
            adminLevel in 7..8 -> GeoBoundaryLevel.ADM3
            else -> GeoBoundaryLevel.ADM4
        }
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
