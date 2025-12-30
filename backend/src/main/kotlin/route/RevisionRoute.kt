package route

import Route
import formatter
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import me.matsumo.travelog.core.repository.GeoBoundaryRepository
import org.koin.ktor.ext.inject

fun Application.revisionRoute() {
    val geoBoundaryRepository by inject<GeoBoundaryRepository>()

    routing {
        get<Route.Revision> {
            call.respondText(formatter.encodeToString(geoBoundaryRepository.getAdmins("JPN")))
            // call.respondText(System.getProperty("REVISION", "Revision not found."))
        }
    }
}
