import io.ktor.resources.Resource
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.resources.Resources
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import route.geoJsonRoute
import route.revisionRoute

val logger = KtorSimpleLogger("matsumo-me-KMP")

val formatter = Json {
    isLenient = true
    prettyPrint = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    explicitNulls = false
}

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(formatter)
    }
    install(Resources)
    install(CallLogging)

    initKoin()
    routes()
}

fun Application.routes() {
    geoJsonRoute()
    revisionRoute()
}

@Serializable
sealed interface Route {

    @Serializable
    @Resource("/revision")
    data object Revision : Route
}