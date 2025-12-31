package me.matsumo.travelog.core.repository

import kotlinx.coroutines.coroutineScope
import me.matsumo.travelog.core.datasource.GeoBoundaryDataSource
import me.matsumo.travelog.core.datasource.NominatimDataSource
import me.matsumo.travelog.core.datasource.OverpassDataSource
import me.matsumo.travelog.core.datasource.WikipediaDataSource
import me.matsumo.travelog.core.model.geo.EnrichedRegion
import me.matsumo.travelog.core.model.geo.GeoBoundaryLevel
import me.matsumo.travelog.core.model.geo.GeoJsonData
import me.matsumo.travelog.core.model.geo.OverpassResult
import me.matsumo.travelog.core.model.geo.toIso3CountryCode

class GeoBoundaryRepository(
    private val geoBoundaryDataSource: GeoBoundaryDataSource,
    private val nominatimDataSource: NominatimDataSource,
    private val overpassDataSource: OverpassDataSource,
    private val wikipediaDataSource: WikipediaDataSource,
    private val geoBoundaryMapper: GeoBoundaryMapper,
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

    suspend fun getThumbnailUrl(wikipedia: String): String? {
        val lang = wikipedia.substringBefore(':')
        val title = wikipedia.substringAfter(':')

        if (lang.isEmpty() || title.isEmpty()) {
            return null
        }

        return wikipediaDataSource.getThumbnailUrl(lang, title)
    }

    suspend fun getEnrichedAdmins(country: String, query: String?): List<EnrichedRegion> = coroutineScope {
        val iso3CountryCode = country.toIso3CountryCode() ?: error("Unknown county code.")
        val adm1GeoJson = getPolygon(iso3CountryCode, GeoBoundaryLevel.ADM1)
        val adm2GeoJson = getPolygon(iso3CountryCode, GeoBoundaryLevel.ADM2)

        val adm1Regions = geoBoundaryMapper.mapAdm1Regions(adm1GeoJson)
        val adm2Regions = geoBoundaryMapper.mapAdm2Regions(adm2GeoJson)

        geoBoundaryMapper.linkAdm2ToAdm1(adm1Regions, adm2Regions)

        val targetAdm1 = geoBoundaryMapper.findTargetAdm1(adm1Regions, query)
        val matchedElements = targetAdm1?.let { adm1 ->
            val overpassElements = runCatching {
                val locationQuery = query?.takeIf { it.isNotBlank() } ?: adm1.name
                getAdmins(locationQuery)
            }.getOrElse { emptyList() }

            geoBoundaryMapper.matchAdm2WithOverpass(adm1.children, overpassElements)
        }.orEmpty()

        targetAdm1?.children
            ?.mapIndexed { index, adm2 ->
                val overpass = matchedElements[adm2.id]
                val displayName = overpass?.tags?.name ?: adm2.name

                val tags = buildMap {
                    put("name", displayName)
                    put("adm2_id", adm2.id)
                    overpass?.tags?.nameEn?.let { put("name:en", it) }
                    overpass?.tags?.nameJa?.let { put("name:ja", it) }
                    overpass?.tags?.wikipedia?.let { put("wikipedia", it) }
                    overpass?.tags?.iso31662?.let { put("ISO3166-2", it) }
                }

                EnrichedRegion(
                    id = index.toLong(),
                    tags = tags,
                    center = adm2.center,
                    polygons = adm2.polygons,
                    thumbnailUrl = null,
                )
            }
            ?.sortedBy { it.tags["name"] ?: "" }
            .orEmpty()
    }
}

