package me.matsumo.travelog.core.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.matsumo.travelog.core.datasource.GeoBoundaryDataSource
import me.matsumo.travelog.core.datasource.NominatimDataSource
import me.matsumo.travelog.core.datasource.OverpassDataSource
import me.matsumo.travelog.core.datasource.WikipediaDataSource
import me.matsumo.travelog.core.model.geo.EnrichedAdm1Regions
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

    suspend fun getEnrichedCountries(country: String): List<GeoBoundaryMapper.Adm1Region> = coroutineScope {
        val iso3CountryCode = country.toIso3CountryCode() ?: error("Unknown county code.")
        val adm1GeoJson = async { getPolygon(iso3CountryCode, GeoBoundaryLevel.ADM1) }
        val adm2GeoJson = async { getPolygon(iso3CountryCode, GeoBoundaryLevel.ADM2) }

        val adm1Regions = geoBoundaryMapper.mapAdm1Regions(adm1GeoJson.await())
        val adm2Regions = geoBoundaryMapper.mapAdm2Regions(adm2GeoJson.await())

        geoBoundaryMapper.linkAdm2ToAdm1(adm1Regions, adm2Regions)
        adm1Regions
    }

    suspend fun getEnrichedAllAdmins(regions: List<GeoBoundaryMapper.Adm1Region>): List<EnrichedAdm1Regions> = coroutineScope {
        regions.map { adm1 ->
            async {
                val overpassElements = runCatching { getAdmins(adm1.name) }.getOrElse { emptyList() }
                val matchedElements = geoBoundaryMapper.matchAdm2WithOverpass(adm1.children, overpassElements)

                val enrichedRegions = adm1.children
                    .map { adm2 ->
                        val overpass = matchedElements[adm2.id]
                        val displayName = overpass?.tags?.name ?: adm2.name

                        EnrichedRegion(
                            name = displayName,
                            adm2Id = adm2.id,
                            nameEn = overpass?.tags?.nameEn,
                            nameJa = overpass?.tags?.nameJa,
                            wikipedia = overpass?.tags?.wikipedia,
                            iso31662 = overpass?.tags?.iso31662,
                            center = adm2.center,
                            polygons = adm2.polygons,
                            thumbnailUrl = null,
                        )
                    }
                    .sortedBy { it.name }

                val thumbnails = enrichedRegions.map { region ->
                    async {
                        region.wikipedia
                            ?.takeIf { it.isNotBlank() }
                            ?.let { wiki ->
                                runCatching { getThumbnailUrl(wiki) }.getOrNull()
                            }
                    }
                }.awaitAll()

                val enrichedWithThumbnails = enrichedRegions.mapIndexed { index, region ->
                    region.copy(thumbnailUrl = thumbnails.getOrNull(index))
                }

                EnrichedAdm1Regions(
                    admId = adm1.id,
                    admName = adm1.name,
                    polygons = adm1.polygons,
                    regions = enrichedWithThumbnails,
                )
            }
        }.awaitAll()
    }
}

