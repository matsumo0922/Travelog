package route

import Route
import formatter
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.geo.GeoJsonProgressEvent
import me.matsumo.travelog.core.repository.GeoBoundaryRepository
import me.matsumo.travelog.core.repository.GeoRegionRepository
import org.koin.ktor.ext.inject

fun Application.geoJsonRoute() {
    val geoBoundaryRepository by inject<GeoBoundaryRepository>()
    val geoRegionRepository by inject<GeoRegionRepository>()

    routing {
        get<Route.GeoJson> { geoJson ->
            suspendRunCatching {
                val regions = geoBoundaryRepository.getEnrichedCountries(geoJson.country)
                val admins = geoBoundaryRepository.getEnrichedAllAdmins(regions)

                geoRegionRepository.upsertRegionGroups(admins)
            }.onSuccess {
                call.respond(HttpStatusCode.OK, "OK")
            }.onFailure {
                call.respond(HttpStatusCode.InternalServerError, it.message ?: "Failed to upsert regions.")
            }
        }
    }
}

fun Application.geoJsonStreamRoute() {
    val geoBoundaryRepository by inject<GeoBoundaryRepository>()
    val geoRegionRepository by inject<GeoRegionRepository>()

    routing {
        sse("/geojson/{country}/stream") {
            val country = call.parameters["country"]
            if (country.isNullOrBlank()) {
                val errorEvent = GeoJsonProgressEvent.Error("Country parameter is required")
                send(ServerSentEvent(data = formatter.encodeToString(errorEvent), event = "progress"))
                return@sse
            }

            try {
                val regions = geoBoundaryRepository.getEnrichedCountries(country)

                val startedEvent = GeoJsonProgressEvent.Started(totalRegions = regions.size)
                send(ServerSentEvent(data = formatter.encodeToString(startedEvent), event = "progress"))

                var successCount = 0
                var failCount = 0

                geoBoundaryRepository.getEnrichedAllAdminsAsFlow(regions).collect { (index, result) ->
                    result
                        .onSuccess { group ->
                            runCatching { geoRegionRepository.upsertRegionGroup(group) }
                                .onSuccess {
                                    successCount++
                                    val event = GeoJsonProgressEvent.RegionCompleted(
                                        index = index,
                                        regionName = group.admName,
                                        success = true,
                                    )
                                    send(ServerSentEvent(data = formatter.encodeToString(event), event = "progress"))
                                }
                                .onFailure { e ->
                                    failCount++
                                    val event = GeoJsonProgressEvent.RegionCompleted(
                                        index = index,
                                        regionName = group.admName,
                                        success = false,
                                        errorMessage = e.message,
                                    )
                                    send(ServerSentEvent(data = formatter.encodeToString(event), event = "progress"))
                                }
                        }
                        .onFailure { e ->
                            failCount++
                            val regionName = regions.getOrNull(index)?.name ?: "Unknown"
                            val event = GeoJsonProgressEvent.RegionCompleted(
                                index = index,
                                regionName = regionName,
                                success = false,
                                errorMessage = e.message,
                            )
                            send(ServerSentEvent(data = formatter.encodeToString(event), event = "progress"))
                        }
                }

                val completedEvent = GeoJsonProgressEvent.Completed(
                    successCount = successCount,
                    failCount = failCount,
                )
                send(ServerSentEvent(data = formatter.encodeToString(completedEvent), event = "progress"))
            } catch (e: Exception) {
                val errorEvent = GeoJsonProgressEvent.Error(e.message ?: "Unknown error occurred")
                send(ServerSentEvent(data = formatter.encodeToString(errorEvent), event = "progress"))
            }
        }
    }
}
