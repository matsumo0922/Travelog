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

        targetAdm1?.children
            ?.sortedBy { it.name }
            ?.mapIndexed { index, adm2 ->
                EnrichedRegion(
                    id = index.toLong(),
                    tags = mapOf("name" to adm2.name),
                    center = adm2.center,
                    polygons = adm2.polygons,
                    thumbnailUrl = null,
                )
            }
            .orEmpty()
    }
}

