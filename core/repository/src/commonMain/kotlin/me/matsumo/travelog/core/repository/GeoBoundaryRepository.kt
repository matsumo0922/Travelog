package me.matsumo.travelog.core.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.datasource.GeoBoundaryDataSource
import me.matsumo.travelog.core.datasource.NominatimDataSource
import me.matsumo.travelog.core.datasource.OverpassDataSource
import me.matsumo.travelog.core.datasource.WikipediaDataSource
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.model.geo.GeoBoundaryLevel
import me.matsumo.travelog.core.model.geo.GeoJsonData
import me.matsumo.travelog.core.model.geo.OverpassResult
import me.matsumo.travelog.core.model.geo.toIso3CountryCode
import me.matsumo.travelog.core.model.geo.toPolygons

/**
 * ADM1処理の進捗イベント
 */
sealed interface Adm1ProcessingEvent {
    /**
     * 処理開始イベント（permit取得時点で発火）
     */
    data class Started(
        val index: Int,
        val regionName: String,
        val adm2Count: Int,
    ) : Adm1ProcessingEvent

    /**
     * 処理完了イベント
     */
    data class Completed(
        val index: Int,
        val result: Result<GeoArea>,
    ) : Adm1ProcessingEvent
}

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

    /**
     * Get ADM0 (country) level GeoArea.
     */
    suspend fun getCountryArea(countryCode: String, countryInfo: CountryInfo? = null): GeoArea = coroutineScope {
        Napier.d(tag = LOG_TAG) { "getCountryArea: Start - countryCode=$countryCode" }

        val iso3CountryCode = countryCode.toIso3CountryCode() ?: error("Unknown country code.")
        val adm0GeoJson = getPolygon(iso3CountryCode, GeoBoundaryLevel.ADM0)

        val polygons = adm0GeoJson.features.flatMap { feature ->
            feature.geometry.toPolygons()
        }

        geoBoundaryMapper.createCountryArea(
            countryCode = countryCode,
            name = countryInfo?.name ?: countryCode,
            nameEn = countryInfo?.nameEn,
            nameJa = countryInfo?.nameJa,
            polygons = polygons,
            wikipedia = countryInfo?.wikipedia,
            thumbnailUrl = countryInfo?.thumbnailUrl,
        )
    }

    /**
     * Country information for ADM0 level.
     */
    data class CountryInfo(
        val name: String,
        val nameEn: String?,
        val nameJa: String?,
        val wikipedia: String?,
        val thumbnailUrl: String?,
    )

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
        Napier.d(tag = LOG_TAG) { "getEnrichedCountries: Start - country=$country" }

        val iso3CountryCode = country.toIso3CountryCode() ?: error("Unknown county code.")
        val adm1GeoJson = async { getPolygon(iso3CountryCode, GeoBoundaryLevel.ADM1) }
        val adm2GeoJson = async { getPolygon(iso3CountryCode, GeoBoundaryLevel.ADM2) }

        val adm1Regions = geoBoundaryMapper.mapAdm1Regions(adm1GeoJson.await())
        val adm2Regions = geoBoundaryMapper.mapAdm2Regions(adm2GeoJson.await())
        Napier.d(tag = LOG_TAG) {
            "getEnrichedCountries: Mapped ${adm1Regions.size} ADM1 regions, ${adm2Regions.size} ADM2 regions"
        }

        geoBoundaryMapper.linkAdm2ToAdm1(adm1Regions, adm2Regions)
        Napier.d(tag = LOG_TAG) { "getEnrichedCountries: Completed - linked ADM2 to ADM1" }

        adm1Regions
    }

    suspend fun getEnrichedAllAdmins(
        countryCode: String,
        regions: List<GeoBoundaryMapper.Adm1Region>,
    ): List<GeoArea> = coroutineScope {
        Napier.d(tag = LOG_TAG) { "getEnrichedAllAdmins: Start - processing ${regions.size} ADM1 regions" }

        regions.mapIndexed { index, adm1 ->
            async {
                processAdm1RegionToGeoArea(adm1, countryCode, index, regions.size)
            }
        }.awaitAll().also {
            Napier.d(tag = LOG_TAG) { "getEnrichedAllAdmins: Completed - processed ${it.size} ADM1 regions" }
        }
    }

    fun getEnrichedAllAdminsAsFlow(
        countryCode: String,
        regions: List<GeoBoundaryMapper.Adm1Region>,
        maxConcurrent: Int = 3,
    ): Flow<Adm1ProcessingEvent> = channelFlow {
        Napier.d(tag = LOG_TAG) {
            "getEnrichedAllAdminsAsFlow: Processing ${regions.size} regions with concurrency=$maxConcurrent"
        }

        val semaphore = Semaphore(maxConcurrent)

        coroutineScope {
            regions.mapIndexed { index, adm1 ->
                launch {
                    semaphore.withPermit {
                        // permit取得直後に Started を emit（UIが即座に「Processing...」に変わる）
                        Napier.d(tag = LOG_TAG) {
                            "getEnrichedAllAdminsAsFlow: [${index + 1}/${regions.size}] ${adm1.name} - started"
                        }
                        send(Adm1ProcessingEvent.Started(index, adm1.name, adm1.children.size))

                        val result = runCatching { processAdm1RegionToGeoArea(adm1, countryCode, index, regions.size) }

                        result.onSuccess {
                            Napier.d(tag = LOG_TAG) { "getEnrichedAllAdminsAsFlow: [${adm1.name}] Completed successfully" }
                        }.onFailure { e ->
                            Napier.e(tag = LOG_TAG, throwable = e) { "getEnrichedAllAdminsAsFlow: [${adm1.name}] Failed" }
                        }

                        // 処理完了時に Completed を emit
                        send(Adm1ProcessingEvent.Completed(index, result))
                    }
                }
            }
        }

        Napier.d(tag = LOG_TAG) { "getEnrichedAllAdminsAsFlow: Flow completed" }
    }

    private suspend fun processAdm1RegionToGeoArea(
        adm1: GeoBoundaryMapper.Adm1Region,
        countryCode: String,
        index: Int,
        total: Int,
    ): GeoArea = coroutineScope {
        Napier.d(tag = LOG_TAG) {
            "processAdm1RegionToGeoArea: [${index + 1}/$total] ${adm1.name} - start (${adm1.children.size} ADM2 regions)"
        }

        val overpassElements = runCatching { getAdmins(adm1.name) }
            .onFailure { Napier.w(tag = LOG_TAG, throwable = it) { "processAdm1RegionToGeoArea: [${adm1.name}] Overpass fetch failed" } }
            .getOrElse { emptyList() }
            .toMutableList()

        // ADM1 enriched data from Overpass
        val adm1Element = overpassElements.find { it.type == "area" }?.also {
            overpassElements.remove(it)
        }

        val adm1EnrichedData = adm1Element?.let {
            val thumbnailUrl = suspendRunCatching {
                it.tags.wikipedia?.let { wikipedia -> getThumbnailUrl(wikipedia) }
            }.getOrNull()

            GeoBoundaryMapper.EnrichedAdm1Data(
                name = adm1.name, // Always use original GeoJSON name to avoid city name overwrite
                nameEn = it.tags.nameEn,
                nameJa = it.tags.nameJa,
                wikipedia = it.tags.wikipedia,
                thumbnailUrl = thumbnailUrl,
            )
        }

        val matchedElements = geoBoundaryMapper.matchAdm2WithOverpass(adm1.children, overpassElements)
        Napier.d(tag = LOG_TAG) {
            "processAdm1RegionToGeoArea: [${adm1.name}] Matched ${matchedElements.size}/${adm1.children.size} with Overpass"
        }

        // Process ADM2 children
        val adm2Children = adm1.children.mapIndexed { idx, adm2 ->
            val overpass = matchedElements[adm2.id]
            val displayName = overpass?.tags?.name ?: adm2.name

            val enrichedData = GeoBoundaryMapper.EnrichedAdm2Data(
                name = displayName,
                nameEn = overpass?.tags?.nameEn,
                nameJa = overpass?.tags?.nameJa,
                isoCode = overpass?.tags?.iso31662,
                wikipedia = overpass?.tags?.wikipedia,
                thumbnailUrl = null,
            )

            Triple(idx, adm2, enrichedData)
        }.sortedBy { it.third.name }

        // Fetch thumbnails for ADM2
        val regionsWithWikipedia = adm2Children.count { !it.third.wikipedia.isNullOrBlank() }
        val thumbnails = adm2Children.map { (_, _, enrichedData) ->
            async {
                enrichedData.wikipedia
                    ?.takeIf { it.isNotBlank() }
                    ?.let { wiki ->
                        runCatching { getThumbnailUrl(wiki) }.getOrNull()
                    }
            }
        }.awaitAll()

        val successfulThumbnails = thumbnails.count { it != null }
        Napier.d(tag = LOG_TAG) {
            "processAdm1RegionToGeoArea: [${adm1.name}] Fetched $successfulThumbnails/$regionsWithWikipedia thumbnails"
        }

        // Convert to GeoArea children
        val geoAreaChildren = adm2Children.mapIndexed { idx, (_, adm2, enrichedData) ->
            val thumbnailUrl = thumbnails.getOrNull(idx)
            geoBoundaryMapper.toGeoArea(
                adm2 = adm2,
                countryCode = countryCode,
                enrichedData = enrichedData.copy(thumbnailUrl = thumbnailUrl),
            )
        }

        // Create ADM1 GeoArea with children
        geoBoundaryMapper.toGeoArea(
            adm1 = adm1,
            countryCode = countryCode,
            enrichedData = adm1EnrichedData,
            children = geoAreaChildren,
        )
    }

    companion object {
        private const val LOG_TAG = "GeoBoundaryRepository"
    }
}
