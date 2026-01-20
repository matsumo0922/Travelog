import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.resources.Resource
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.basic
import io.ktor.server.auth.bearer
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.resources.Resources
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.li
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.title
import kotlinx.html.ul
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import route.batchApiRoute
import route.geoJsonAllCountriesStreamRoute
import route.geoJsonRoute
import route.geoJsonStreamRoute
import route.geoNameEnrichmentRoute
import route.geoNameEnrichmentStreamRoute
import route.geoNamesAllCountriesStreamRoute
import route.revisionRoute

val logger = KtorSimpleLogger("matsumo-me-KMP")

val formatter = Json {
    isLenient = true
    prettyPrint = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    explicitNulls = false
    classDiscriminator = "_type"
}

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    Napier.base(DebugAntilog())

    install(ContentNegotiation) {
        json(formatter)
    }
    install(Resources)
    install(CallLogging)
    install(SSE)
    install(Authentication) {
        basic("auth-basic") {
            realm = "Travelog API"
            validate { credentials ->
                val username = System.getenv("BASIC_AUTH_USERNAME")
                val password = System.getenv("BASIC_AUTH_PASSWORD")

                if (credentials.name == username && credentials.password == password) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }

        bearer("auth-api-key") {
            realm = "Travelog Batch API"
            authenticate { credential ->
                val expectedKey = System.getenv("BATCH_API_KEY")
                if (!expectedKey.isNullOrBlank() && credential.token == expectedKey) {
                    UserIdPrincipal("batch-api")
                } else {
                    null
                }
            }
        }
    }

    initKoin()
    routes()
}

fun Application.routes() {
    routing {
        staticResources("/static", "static")
        get("/") {
            call.respondHtml {
                head {
                    meta(charset = "UTF-8")
                    title("Travelog Backend")
                    script(src = "https://cdn.tailwindcss.com") {}
                }
                body(classes = "bg-gray-100 min-h-screen") {
                    div(classes = "max-w-4xl mx-auto py-10 px-5") {
                        h1(classes = "text-3xl font-bold text-gray-800 mb-4") { +"Travelog Backend" }
                        p(classes = "text-gray-600 mb-6") { +"Available endpoints:" }
                        ul(classes = "space-y-3") {
                            li {
                                a(href = "/geojson", classes = "text-blue-600 hover:text-blue-800 underline") {
                                    +"/geojson"
                                }
                                p(classes = "text-gray-500 text-sm ml-4") { +"GeoJSON data import and management" }
                            }
                            li {
                                a(href = "/geo-names", classes = "text-blue-600 hover:text-blue-800 underline") {
                                    +"/geo-names"
                                }
                                p(classes = "text-gray-500 text-sm ml-4") { +"Geo name enrichment using Gemini AI" }
                            }
                            li {
                                a(href = "/revision", classes = "text-blue-600 hover:text-blue-800 underline") {
                                    +"/revision"
                                }
                                p(classes = "text-gray-500 text-sm ml-4") { +"API revision information" }
                            }
                        }
                    }
                }
            }
        }
    }
    geoJsonRoute()
    geoJsonStreamRoute()
    geoJsonAllCountriesStreamRoute()
    geoNameEnrichmentRoute()
    geoNameEnrichmentStreamRoute()
    geoNamesAllCountriesStreamRoute()
    batchApiRoute()
    revisionRoute()
}

@Serializable
sealed interface Route {

    @Serializable
    @Resource("/geojson")
    data object GeoJsonList : Route

    @Serializable
    @Resource("/geojson/{country}")
    data class GeoJson(val country: String) : Route

    @Serializable
    @Resource("/revision")
    data object Revision : Route
}
