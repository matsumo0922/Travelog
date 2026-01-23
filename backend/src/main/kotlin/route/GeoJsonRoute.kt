package route

import Route
import formatter
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.html.respondHtml
import io.ktor.server.resources.get
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.model.geo.GeoJsonProgressEvent
import me.matsumo.travelog.core.model.geo.MultiCountryProgressEvent
import me.matsumo.travelog.core.repository.GeoAreaRepository
import org.koin.ktor.ext.inject
import repository.Adm1ProcessingEvent
import repository.GeoBoundaryMapper
import repository.GeoBoundaryRepository
import ui.geojson.allCountriesProgressPage
import ui.geojson.progressPage
import ui.geojson.regionListPage

fun Application.geoJsonRoute() {
    routing {
        authenticate("auth-basic") {
            get<Route.GeoJsonList> {
                call.respondHtml {
                    regionListPage()
                }
            }
            get<Route.GeoJson> { geoJson ->
                call.respondHtml {
                    progressPage(geoJson.country)
                }
            }
            get("/geojson/all") {
                call.respondHtml {
                    allCountriesProgressPage("geojson")
                }
            }
        }
    }
}

fun Application.geoJsonStreamRoute() {
    val geoBoundaryRepository by inject<GeoBoundaryRepository>()
    val geoAreaRepository by inject<GeoAreaRepository>()

    routing {
        authenticate("auth-basic") {
            sse("/geojson/{country}/stream") {
                val country = call.parameters["country"]
                if (country.isNullOrBlank()) {
                    val errorEvent = GeoJsonProgressEvent.Error("Country parameter is required")
                    send(ServerSentEvent(data = formatter.encodeToString(errorEvent), event = "progress"))
                    return@sse
                }

                val overallStartTime = System.currentTimeMillis()

                try {
                    // Get ADM0 (country) polygon first
                    val supportedRegion = SupportedRegion.all.find { it.code2 == country }
                    val countryInfo = supportedRegion?.let {
                        GeoBoundaryRepository.CountryInfo(
                            name = it.nameEn,
                            nameEn = it.nameEn,
                            nameJa = null,
                            wikipedia = null,
                            thumbnailUrl = it.flagUrl,
                        )
                    }
                    val countryArea = geoBoundaryRepository.getCountryArea(country, countryInfo)

                    // Get ADM1 regions
                    val regions = geoBoundaryRepository.getEnrichedCountries(country)

                    // Build RegionInfo list for initial card display
                    val regionInfoList = buildRegionInfoList(supportedRegion, countryArea, regions)

                    // Total = 1 (country) + ADM1 regions
                    val totalRegions = regions.size + 1
                    val startedEvent = GeoJsonProgressEvent.Started(
                        totalRegions = totalRegions,
                        regions = regionInfoList,
                    )
                    send(ServerSentEvent(data = formatter.encodeToString(startedEvent), event = "progress"))

                    var successCount = 0
                    var failCount = 0
                    var totalAdm2Count = 0

                    // First, upsert the country (ADM0)
                    val adm0StartTime = System.currentTimeMillis()

                    // Send region started event for ADM0
                    val adm0StartedEvent = GeoJsonProgressEvent.RegionStarted(
                        index = 0,
                        regionName = countryArea.name,
                        level = 0,
                        adm2Count = 0,
                    )
                    send(ServerSentEvent(data = formatter.encodeToString(adm0StartedEvent), event = "progress"))

                    runCatching { geoAreaRepository.upsertArea(countryArea) }
                        .onSuccess { countryId ->
                            successCount++
                            val event = GeoJsonProgressEvent.RegionCompleted(
                                index = 0,
                                regionName = countryArea.name,
                                success = true,
                                level = 0,
                                nameEn = countryArea.nameEn,
                                nameJa = countryArea.nameJa,
                                isoCode = countryArea.isoCode,
                                thumbnailUrl = supportedRegion?.flagUrl,
                                wikipedia = countryArea.wikipedia,
                                centerLat = countryArea.center?.lat,
                                centerLon = countryArea.center?.lon,
                                processingTimeMs = System.currentTimeMillis() - adm0StartTime,
                            )
                            send(ServerSentEvent(data = formatter.encodeToString(event), event = "progress"))

                            // Process ADM1 regions
                            geoBoundaryRepository.getEnrichedAllAdminsAsFlow(country, regions).collect { event ->
                                when (event) {
                                    is Adm1ProcessingEvent.Started -> {
                                        val regionStartedEvent = GeoJsonProgressEvent.RegionStarted(
                                            index = event.index + 1,
                                            regionName = event.regionName,
                                            level = 1,
                                            adm2Count = event.adm2Count,
                                        )
                                        send(ServerSentEvent(data = formatter.encodeToString(regionStartedEvent), event = "progress"))
                                    }

                                    is Adm1ProcessingEvent.Completed -> {
                                        val adm1StartTime = System.currentTimeMillis()
                                        val index = event.index
                                        event.result
                                            .onSuccess { adm1GeoArea ->
                                                val areaWithParent = adm1GeoArea.copy(parentId = countryId)

                                                runCatching {
                                                    val adm1Id = geoAreaRepository.upsertArea(areaWithParent)
                                                    var adm2Processed = 0

                                                    if (adm1GeoArea.children.isNotEmpty()) {
                                                        val adm2WithParent = adm1GeoArea.children.map { it.copy(parentId = adm1Id) }

                                                        adm2WithParent.forEachIndexed { adm2Index, adm2 ->
                                                            runCatching { geoAreaRepository.upsertArea(adm2) }
                                                                .onSuccess { adm2Processed++ }

                                                            val progressEvent = GeoJsonProgressEvent.Adm2Progress(
                                                                adm1Index = index + 1,
                                                                processedCount = adm2Index + 1,
                                                                totalCount = adm2WithParent.size,
                                                                currentAdm2Name = adm2.name,
                                                            )
                                                            send(ServerSentEvent(data = formatter.encodeToString(progressEvent), event = "progress"))
                                                        }
                                                    }
                                                    adm2Processed to adm1GeoArea.children.size
                                                }
                                                    .onSuccess { (processedAdm2, totalAdm2) ->
                                                        successCount++
                                                        totalAdm2Count += totalAdm2
                                                        val completedEvent = GeoJsonProgressEvent.RegionCompleted(
                                                            index = index + 1,
                                                            regionName = adm1GeoArea.name,
                                                            success = true,
                                                            level = 1,
                                                            nameEn = adm1GeoArea.nameEn,
                                                            nameJa = adm1GeoArea.nameJa,
                                                            isoCode = adm1GeoArea.isoCode,
                                                            thumbnailUrl = adm1GeoArea.thumbnailUrl,
                                                            wikipedia = adm1GeoArea.wikipedia,
                                                            centerLat = adm1GeoArea.center?.lat,
                                                            centerLon = adm1GeoArea.center?.lon,
                                                            adm2ProcessedCount = processedAdm2,
                                                            adm2TotalCount = totalAdm2,
                                                            processingTimeMs = System.currentTimeMillis() - adm1StartTime,
                                                        )
                                                        send(ServerSentEvent(data = formatter.encodeToString(completedEvent), event = "progress"))
                                                    }
                                                    .onFailure { e ->
                                                        failCount++
                                                        val failedEvent = GeoJsonProgressEvent.RegionCompleted(
                                                            index = index + 1,
                                                            regionName = adm1GeoArea.name,
                                                            success = false,
                                                            errorMessage = e.message,
                                                            level = 1,
                                                            nameEn = adm1GeoArea.nameEn,
                                                            nameJa = adm1GeoArea.nameJa,
                                                            isoCode = adm1GeoArea.isoCode,
                                                            thumbnailUrl = adm1GeoArea.thumbnailUrl,
                                                            processingTimeMs = System.currentTimeMillis() - adm1StartTime,
                                                        )
                                                        send(ServerSentEvent(data = formatter.encodeToString(failedEvent), event = "progress"))
                                                    }
                                            }
                                            .onFailure { e ->
                                                failCount++
                                                val regionName = regions.getOrNull(index)?.name ?: "Unknown"
                                                val failedEvent = GeoJsonProgressEvent.RegionCompleted(
                                                    index = index + 1,
                                                    regionName = regionName,
                                                    success = false,
                                                    errorMessage = e.message,
                                                    level = 1,
                                                )
                                                send(ServerSentEvent(data = formatter.encodeToString(failedEvent), event = "progress"))
                                            }
                                    }
                                }
                            }
                        }
                        .onFailure { e ->
                            failCount++
                            val event = GeoJsonProgressEvent.RegionCompleted(
                                index = 0,
                                regionName = countryArea.name,
                                success = false,
                                errorMessage = e.message,
                                level = 0,
                                processingTimeMs = System.currentTimeMillis() - adm0StartTime,
                            )
                            send(ServerSentEvent(data = formatter.encodeToString(event), event = "progress"))
                        }

                    val completedEvent = GeoJsonProgressEvent.Completed(
                        successCount = successCount,
                        failCount = failCount,
                        totalProcessingTimeMs = System.currentTimeMillis() - overallStartTime,
                        adm2TotalCount = totalAdm2Count,
                    )
                    send(ServerSentEvent(data = formatter.encodeToString(completedEvent), event = "progress"))
                } catch (e: Exception) {
                    val errorEvent = GeoJsonProgressEvent.Error(e.message ?: "Unknown error occurred")
                    send(ServerSentEvent(data = formatter.encodeToString(errorEvent), event = "progress"))
                }
            }
        }
    }
}

/**
 * Build RegionInfo list for initial card display
 */
private fun buildRegionInfoList(
    supportedRegion: SupportedRegion?,
    countryArea: GeoArea,
    regions: List<GeoBoundaryMapper.Adm1Region>,
): List<GeoJsonProgressEvent.RegionInfo> {
    val list = mutableListOf<GeoJsonProgressEvent.RegionInfo>()

    // ADM0 (country)
    list.add(
        GeoJsonProgressEvent.RegionInfo(
            index = 0,
            name = countryArea.name,
            level = 0,
            thumbnailUrl = supportedRegion?.flagUrl,
            adm2Count = 0,
        ),
    )

    // ADM1 regions
    regions.forEachIndexed { index, region ->
        list.add(
            GeoJsonProgressEvent.RegionInfo(
                index = index + 1,
                name = region.name,
                level = 1,
                thumbnailUrl = null,
                adm2Count = region.children.size,
            ),
        )
    }

    return list
}

/**
 * 全国GeoJSON処理用SSEエンドポイント
 */
fun Application.geoJsonAllCountriesStreamRoute() {
    val geoBoundaryRepository by inject<GeoBoundaryRepository>()
    val geoAreaRepository by inject<GeoAreaRepository>()

    routing {
        authenticate("auth-basic") {
            sse("/geojson/all/stream") {
                val overallStartTime = System.currentTimeMillis()
                val countries = SupportedRegion.all

                try {
                    // Send AllStarted event with country list
                    val countryInfoList = countries.mapIndexed { index, region ->
                        MultiCountryProgressEvent.CountryInfo(
                            index = index,
                            code = region.code2,
                            name = region.nameEn,
                            flagUrl = region.flagUrl,
                            regionCount = region.subRegionCount,
                        )
                    }
                    val allStartedEvent = MultiCountryProgressEvent.AllStarted(
                        countries = countryInfoList,
                        totalCountries = countries.size,
                    )
                    send(ServerSentEvent(data = formatter.encodeToString(allStartedEvent), event = "progress"))

                    var totalSuccessCountries = 0
                    var totalFailCountries = 0

                    // Process each country sequentially
                    countries.forEachIndexed { countryIndex, supportedRegion ->
                        val countryStartTime = System.currentTimeMillis()
                        val countryCode = supportedRegion.code2

                        // Send CountryStarted event
                        val countryStartedEvent = MultiCountryProgressEvent.CountryStarted(
                            countryIndex = countryIndex,
                            countryCode = countryCode,
                            countryName = supportedRegion.nameEn,
                        )
                        send(ServerSentEvent(data = formatter.encodeToString(countryStartedEvent), event = "progress"))

                        var countrySuccess = 0
                        var countryFail = 0
                        var countryTotalRegions = 0

                        try {
                            // Get country area
                            val countryInfo = GeoBoundaryRepository.CountryInfo(
                                name = supportedRegion.nameEn,
                                nameEn = supportedRegion.nameEn,
                                nameJa = null,
                                wikipedia = null,
                                thumbnailUrl = supportedRegion.flagUrl,
                            )
                            val countryArea = geoBoundaryRepository.getCountryArea(countryCode, countryInfo)

                            // Get ADM1 regions
                            val regions = geoBoundaryRepository.getEnrichedCountries(countryCode)
                            countryTotalRegions = regions.size + 1

                            // Send inner Started event wrapped in CountryProgress
                            val innerStartedEvent = GeoJsonProgressEvent.Started(
                                totalRegions = countryTotalRegions,
                                regions = buildRegionInfoList(supportedRegion, countryArea, regions),
                            )
                            val wrappedStarted = MultiCountryProgressEvent.CountryProgress(
                                countryIndex = countryIndex,
                                countryCode = countryCode,
                                innerEventJson = formatter.encodeToString(innerStartedEvent),
                                innerEventType = "started",
                            )
                            send(ServerSentEvent(data = formatter.encodeToString(wrappedStarted), event = "progress"))

                            // Upsert country (ADM0)
                            val adm0StartTime = System.currentTimeMillis()

                            // Send ADM0 started event
                            val adm0StartedEvent = GeoJsonProgressEvent.RegionStarted(
                                index = 0,
                                regionName = countryArea.name,
                                level = 0,
                                adm2Count = 0,
                            )
                            val wrappedAdm0Started = MultiCountryProgressEvent.CountryProgress(
                                countryIndex = countryIndex,
                                countryCode = countryCode,
                                innerEventJson = formatter.encodeToString(adm0StartedEvent),
                                innerEventType = "region_started",
                            )
                            send(ServerSentEvent(data = formatter.encodeToString(wrappedAdm0Started), event = "progress"))

                            runCatching { geoAreaRepository.upsertArea(countryArea) }
                                .onSuccess { countryId ->
                                    countrySuccess++

                                    val adm0CompletedEvent = GeoJsonProgressEvent.RegionCompleted(
                                        index = 0,
                                        regionName = countryArea.name,
                                        success = true,
                                        level = 0,
                                        nameEn = countryArea.nameEn,
                                        nameJa = countryArea.nameJa,
                                        isoCode = countryArea.isoCode,
                                        thumbnailUrl = supportedRegion.flagUrl,
                                        wikipedia = countryArea.wikipedia,
                                        centerLat = countryArea.center?.lat,
                                        centerLon = countryArea.center?.lon,
                                        processingTimeMs = System.currentTimeMillis() - adm0StartTime,
                                    )
                                    val wrappedAdm0Completed = MultiCountryProgressEvent.CountryProgress(
                                        countryIndex = countryIndex,
                                        countryCode = countryCode,
                                        innerEventJson = formatter.encodeToString(adm0CompletedEvent),
                                        innerEventType = "region_completed",
                                    )
                                    send(ServerSentEvent(data = formatter.encodeToString(wrappedAdm0Completed), event = "progress"))

                                    // Process ADM1 regions
                                    geoBoundaryRepository.getEnrichedAllAdminsAsFlow(countryCode, regions).collect { event ->
                                        when (event) {
                                            is Adm1ProcessingEvent.Started -> {
                                                val regionStartedEvent = GeoJsonProgressEvent.RegionStarted(
                                                    index = event.index + 1,
                                                    regionName = event.regionName,
                                                    level = 1,
                                                    adm2Count = event.adm2Count,
                                                )
                                                val wrapped = MultiCountryProgressEvent.CountryProgress(
                                                    countryIndex = countryIndex,
                                                    countryCode = countryCode,
                                                    innerEventJson = formatter.encodeToString(regionStartedEvent),
                                                    innerEventType = "region_started",
                                                )
                                                send(ServerSentEvent(data = formatter.encodeToString(wrapped), event = "progress"))
                                            }

                                            is Adm1ProcessingEvent.Completed -> {
                                                val adm1StartTime = System.currentTimeMillis()
                                                val index = event.index

                                                event.result
                                                    .onSuccess { adm1GeoArea ->
                                                        val areaWithParent = adm1GeoArea.copy(parentId = countryId)

                                                        runCatching {
                                                            val adm1Id = geoAreaRepository.upsertArea(areaWithParent)
                                                            var adm2Processed = 0

                                                            if (adm1GeoArea.children.isNotEmpty()) {
                                                                val adm2WithParent = adm1GeoArea.children.map { it.copy(parentId = adm1Id) }
                                                                adm2WithParent.forEachIndexed { adm2Index, adm2 ->
                                                                    runCatching { geoAreaRepository.upsertArea(adm2) }
                                                                        .onSuccess { adm2Processed++ }

                                                                    val progressEvent = GeoJsonProgressEvent.Adm2Progress(
                                                                        adm1Index = index + 1,
                                                                        processedCount = adm2Index + 1,
                                                                        totalCount = adm2WithParent.size,
                                                                        currentAdm2Name = adm2.name,
                                                                    )
                                                                    val wrapped = MultiCountryProgressEvent.CountryProgress(
                                                                        countryIndex = countryIndex,
                                                                        countryCode = countryCode,
                                                                        innerEventJson = formatter.encodeToString(progressEvent),
                                                                        innerEventType = "adm2_progress",
                                                                    )
                                                                    send(
                                                                        ServerSentEvent(
                                                                            data = formatter.encodeToString(wrapped),
                                                                            event = "progress",
                                                                        ),
                                                                    )
                                                                }
                                                            }
                                                            adm2Processed to adm1GeoArea.children.size
                                                        }
                                                            .onSuccess { (processedAdm2, totalAdm2) ->
                                                                countrySuccess++
                                                                val completedEvent = GeoJsonProgressEvent.RegionCompleted(
                                                                    index = index + 1,
                                                                    regionName = adm1GeoArea.name,
                                                                    success = true,
                                                                    level = 1,
                                                                    nameEn = adm1GeoArea.nameEn,
                                                                    nameJa = adm1GeoArea.nameJa,
                                                                    isoCode = adm1GeoArea.isoCode,
                                                                    thumbnailUrl = adm1GeoArea.thumbnailUrl,
                                                                    wikipedia = adm1GeoArea.wikipedia,
                                                                    centerLat = adm1GeoArea.center?.lat,
                                                                    centerLon = adm1GeoArea.center?.lon,
                                                                    adm2ProcessedCount = processedAdm2,
                                                                    adm2TotalCount = totalAdm2,
                                                                    processingTimeMs = System.currentTimeMillis() - adm1StartTime,
                                                                )
                                                                val wrapped = MultiCountryProgressEvent.CountryProgress(
                                                                    countryIndex = countryIndex,
                                                                    countryCode = countryCode,
                                                                    innerEventJson = formatter.encodeToString(completedEvent),
                                                                    innerEventType = "region_completed",
                                                                )
                                                                send(ServerSentEvent(data = formatter.encodeToString(wrapped), event = "progress"))
                                                            }
                                                            .onFailure { e ->
                                                                countryFail++
                                                                val failedEvent = GeoJsonProgressEvent.RegionCompleted(
                                                                    index = index + 1,
                                                                    regionName = adm1GeoArea.name,
                                                                    success = false,
                                                                    errorMessage = e.message,
                                                                    level = 1,
                                                                    nameEn = adm1GeoArea.nameEn,
                                                                    nameJa = adm1GeoArea.nameJa,
                                                                    isoCode = adm1GeoArea.isoCode,
                                                                    thumbnailUrl = adm1GeoArea.thumbnailUrl,
                                                                    processingTimeMs = System.currentTimeMillis() - adm1StartTime,
                                                                )
                                                                val wrapped = MultiCountryProgressEvent.CountryProgress(
                                                                    countryIndex = countryIndex,
                                                                    countryCode = countryCode,
                                                                    innerEventJson = formatter.encodeToString(failedEvent),
                                                                    innerEventType = "region_completed",
                                                                )
                                                                send(ServerSentEvent(data = formatter.encodeToString(wrapped), event = "progress"))
                                                            }
                                                    }
                                                    .onFailure { e ->
                                                        countryFail++
                                                        val regionName = regions.getOrNull(index)?.name ?: "Unknown"
                                                        val failedEvent = GeoJsonProgressEvent.RegionCompleted(
                                                            index = index + 1,
                                                            regionName = regionName,
                                                            success = false,
                                                            errorMessage = e.message,
                                                            level = 1,
                                                        )
                                                        val wrapped = MultiCountryProgressEvent.CountryProgress(
                                                            countryIndex = countryIndex,
                                                            countryCode = countryCode,
                                                            innerEventJson = formatter.encodeToString(failedEvent),
                                                            innerEventType = "region_completed",
                                                        )
                                                        send(ServerSentEvent(data = formatter.encodeToString(wrapped), event = "progress"))
                                                    }
                                            }
                                        }
                                    }
                                }
                                .onFailure { e ->
                                    countryFail++
                                    val adm0FailedEvent = GeoJsonProgressEvent.RegionCompleted(
                                        index = 0,
                                        regionName = countryArea.name,
                                        success = false,
                                        errorMessage = e.message,
                                        level = 0,
                                        processingTimeMs = System.currentTimeMillis() - adm0StartTime,
                                    )
                                    val wrapped = MultiCountryProgressEvent.CountryProgress(
                                        countryIndex = countryIndex,
                                        countryCode = countryCode,
                                        innerEventJson = formatter.encodeToString(adm0FailedEvent),
                                        innerEventType = "region_completed",
                                    )
                                    send(ServerSentEvent(data = formatter.encodeToString(wrapped), event = "progress"))
                                }

                            // Send country completed event
                            val isCountrySuccess = countryFail == 0
                            if (isCountrySuccess) totalSuccessCountries++ else totalFailCountries++

                            val countryCompletedEvent = MultiCountryProgressEvent.CountryCompleted(
                                countryIndex = countryIndex,
                                countryCode = countryCode,
                                countryName = supportedRegion.nameEn,
                                success = isCountrySuccess,
                                successCount = countrySuccess,
                                failCount = countryFail,
                                totalRegions = countryTotalRegions,
                                processingTimeMs = System.currentTimeMillis() - countryStartTime,
                            )
                            send(ServerSentEvent(data = formatter.encodeToString(countryCompletedEvent), event = "progress"))
                        } catch (e: Exception) {
                            totalFailCountries++
                            val countryCompletedEvent = MultiCountryProgressEvent.CountryCompleted(
                                countryIndex = countryIndex,
                                countryCode = countryCode,
                                countryName = supportedRegion.nameEn,
                                success = false,
                                errorMessage = e.message,
                                processingTimeMs = System.currentTimeMillis() - countryStartTime,
                            )
                            send(ServerSentEvent(data = formatter.encodeToString(countryCompletedEvent), event = "progress"))
                        }
                    }

                    // Send AllCompleted event
                    val allCompletedEvent = MultiCountryProgressEvent.AllCompleted(
                        totalCountries = countries.size,
                        successCount = totalSuccessCountries,
                        failCount = totalFailCountries,
                        totalTimeMs = System.currentTimeMillis() - overallStartTime,
                    )
                    send(ServerSentEvent(data = formatter.encodeToString(allCompletedEvent), event = "progress"))
                } catch (e: Exception) {
                    val errorEvent = MultiCountryProgressEvent.Error(
                        message = e.message ?: "Unknown error occurred",
                    )
                    send(ServerSentEvent(data = formatter.encodeToString(errorEvent), event = "progress"))
                }
            }
        }
    }
}
