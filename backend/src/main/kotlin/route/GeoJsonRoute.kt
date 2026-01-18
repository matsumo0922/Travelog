package route

import Route
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import me.matsumo.travelog.core.common.suspendRunCatching
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
