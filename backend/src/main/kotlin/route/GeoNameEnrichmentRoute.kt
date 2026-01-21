package route

import formatter
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.html.body
import kotlinx.html.p
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.geo.MultiCountryProgressEvent
import model.GeoNameEnrichmentEvent
import org.koin.ktor.ext.inject
import repository.GeoNameEnrichmentRepository
import ui.enrichment.geoNameEnrichmentListPage
import ui.enrichment.geoNameEnrichmentProgressPage
import ui.enrichment.geoNamesAllCountriesProgressPage
import ui.enrichment.missingNamesPage

fun Application.geoNameEnrichmentRoute() {
    val repository by inject<GeoNameEnrichmentRepository>()

    routing {
        authenticate("auth-basic") {
            get("/geo-names") {
                call.respondHtml {
                    geoNameEnrichmentListPage()
                }
            }
            get("/geo-names/enrich/{country}") {
                val country = call.parameters["country"] ?: "JP"
                val level = call.request.queryParameters["level"]?.toIntOrNull()
                call.respondHtml {
                    geoNameEnrichmentProgressPage(country, level)
                }
            }
            get("/geo-names/enrich/all") {
                call.respondHtml {
                    geoNamesAllCountriesProgressPage()
                }
            }
        }
    }
}

fun Application.geoNameEnrichmentStreamRoute() {
    val repository by inject<GeoNameEnrichmentRepository>()

    routing {
        authenticate("auth-basic") {
            // Get missing names for a country
            get("/geo-names/missing/{country}") {
                val country = call.parameters["country"]
                if (country.isNullOrBlank()) {
                    call.respondHtml {
                        body {
                            p { +"Country parameter is required" }
                        }
                    }
                    return@get
                }

                val level = call.request.queryParameters["level"]?.toIntOrNull()
                val levelLabel = level?.let { "Level $it" } ?: "All Levels"

                runCatching {
                    val areas = repository.getAreasWithMissingNames(country, level)
                    call.respondHtml {
                        missingNamesPage(country, levelLabel, areas)
                    }
                }.onFailure { e ->
                    call.respondHtml {
                        body {
                            p { +"Error: ${e.message}" }
                        }
                    }
                }
            }

            // SSE endpoint for enrichment
            sse("/geo-names/enrich/{country}/stream") {
                val country = call.parameters["country"]
                if (country.isNullOrBlank()) {
                    val errorEvent = GeoNameEnrichmentEvent.Error("Country parameter is required")
                    send(ServerSentEvent(data = formatter.encodeToString(errorEvent), event = "progress"))
                    return@sse
                }

                val level = call.request.queryParameters["level"]?.toIntOrNull()
                val dryRun = call.request.queryParameters["dryRun"]?.toBoolean() ?: false
                val batchSize = call.request.queryParameters["batchSize"]?.toIntOrNull() ?: 10

                try {
                    repository.enrichGeoNamesAsFlow(
                        countryCode = country,
                        level = level,
                        batchSize = batchSize,
                        dryRun = dryRun,
                    ).collect { event ->
                        send(ServerSentEvent(data = formatter.encodeToString(event), event = "progress"))
                    }
                } catch (e: Exception) {
                    val errorEvent = GeoNameEnrichmentEvent.Error(e.message ?: "Unknown error occurred")
                    send(ServerSentEvent(data = formatter.encodeToString(errorEvent), event = "progress"))
                }
            }
        }
    }
}

/**
 * 全国名前補完処理用SSEエンドポイント
 */
fun Application.geoNamesAllCountriesStreamRoute() {
    val repository by inject<GeoNameEnrichmentRepository>()

    routing {
        authenticate("auth-basic") {
            sse("/geo-names/enrich/all/stream") {
                val overallStartTime = System.currentTimeMillis()
                val countries = SupportedRegion.all
                val batchSize = call.request.queryParameters["batchSize"]?.toIntOrNull() ?: 10
                val dryRun = call.request.queryParameters["dryRun"]?.toBoolean() ?: false

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

                        try {
                            // Process enrichment for this country
                            repository.enrichGeoNamesAsFlow(
                                countryCode = countryCode,
                                level = null,
                                batchSize = batchSize,
                                dryRun = dryRun,
                            ).collect { event ->
                                // Get the event type
                                val eventType = when (event) {
                                    is GeoNameEnrichmentEvent.Started -> event.type
                                    is GeoNameEnrichmentEvent.BatchProcessed -> event.type
                                    is GeoNameEnrichmentEvent.ItemResult -> event.type
                                    is GeoNameEnrichmentEvent.Completed -> event.type
                                    is GeoNameEnrichmentEvent.Error -> event.type
                                }

                                // Wrap inner event
                                val wrapped = MultiCountryProgressEvent.CountryProgress(
                                    countryIndex = countryIndex,
                                    countryCode = countryCode,
                                    innerEventJson = formatter.encodeToString(event),
                                    innerEventType = eventType,
                                )
                                send(ServerSentEvent(data = formatter.encodeToString(wrapped), event = "progress"))

                                // Track success/fail counts
                                when (event) {
                                    is GeoNameEnrichmentEvent.Completed -> {
                                        countrySuccess = event.successCount
                                        countryFail = event.failedCount
                                    }

                                    else -> {}
                                }
                            }

                            // Determine country success based on whether we had critical errors
                            val isCountrySuccess = countryFail == 0 || countrySuccess > 0
                            if (isCountrySuccess) totalSuccessCountries++ else totalFailCountries++

                            val countryCompletedEvent = MultiCountryProgressEvent.CountryCompleted(
                                countryIndex = countryIndex,
                                countryCode = countryCode,
                                countryName = supportedRegion.nameEn,
                                success = isCountrySuccess,
                                successCount = countrySuccess,
                                failCount = countryFail,
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
