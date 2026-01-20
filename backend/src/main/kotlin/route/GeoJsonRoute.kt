package route

import Route
import formatter
import io.ktor.server.application.Application
import io.ktor.server.html.respondHtml
import io.ktor.server.resources.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.html.BODY
import kotlinx.html.DIV
import kotlinx.html.HEAD
import kotlinx.html.HTML
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.meta
import kotlinx.html.onClick
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.geo.GeoJsonProgressEvent
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.GeoBoundaryRepository
import org.koin.ktor.ext.inject

fun Application.geoJsonRoute() {
    routing {
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
    }
}

// ページコンポーネント
private fun HTML.regionListPage() {
    head {
        meta(charset = "UTF-8")
        title("GeoJSON Processing")
        tailwindCdn()
    }
    body(classes = "max-w-4xl mx-auto py-10 px-5 font-sans") {
        h1(classes = "text-gray-800 text-2xl font-bold mb-4") { +"GeoJSON Processing" }
        p(classes = "text-gray-600 mb-6") { +"Select a region to process:" }
        regionList()
    }
}

private fun HTML.progressPage(country: String) {
    head {
        meta(charset = "UTF-8")
        title("GeoJSON Processing - $country")
        tailwindCdn()
        logStyles()
    }
    body(classes = "max-w-4xl mx-auto py-10 px-5 font-sans") {
        h1(classes = "text-gray-800 text-2xl font-bold mb-4") { +"GeoJSON Processing: $country" }
        controlButtons(country)
        progressContainer()
        logContainer()
        script(src = "/static/js/geojson-progress.js") {}
    }
}

// スタイルコンポーネント
private fun HEAD.tailwindCdn() {
    script(src = "https://cdn.tailwindcss.com") {}
}

private fun HEAD.logStyles() {
    style {
        unsafe {
            raw(
                """
                .log-entry.success { color: #4CAF50; }
                .log-entry.error { color: #f44336; }
                .log-entry.info { color: #2196F3; }
                """.trimIndent(),
            )
        }
    }
}

// UI コンポーネント
private fun BODY.regionList() {
    div(classes = "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4") {
        SupportedRegion.all.forEach { region ->
            regionCard(region)
        }
    }
}

private fun DIV.regionCard(region: SupportedRegion) {
    a(
        href = "/geojson/${region.code2}",
        classes = "bg-gray-100 hover:bg-gray-200 rounded-lg p-4 no-underline text-gray-800 transition-colors flex items-center gap-3"
    ) {
        img(
            src = region.flagUrl,
            alt = region.nameEn,
            classes = "w-12 h-9 object-cover rounded",
        )
        div(classes = "flex-1") {
            div(classes = "font-semibold mb-1") { +region.nameEn }
            div(classes = "text-xs text-gray-500") { +"${region.subRegionCount} regions" }
        }
    }
}

private fun BODY.controlButtons(country: String) {
    div(classes = "flex gap-3 mb-5") {
        button(
            classes = "bg-green-500 hover:bg-green-600 text-white font-semibold py-2 px-6 rounded-lg " +
                    "transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed",
        ) {
            id = "startBtn"
            onClick = "startProcessing('$country')"
            +"Start"
        }
        button(
            classes = "bg-red-500 hover:bg-red-600 text-white font-semibold py-2 px-6 rounded-lg " +
                    "transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed",
        ) {
            id = "stopBtn"
            onClick = "stopProcessing()"
            attributes["disabled"] = "true"
            +"Stop"
        }
    }
}

private fun BODY.progressContainer() {
    div(classes = "bg-gray-100 rounded-lg p-5 my-5") {
        div(classes = "bg-gray-300 rounded h-6 overflow-hidden") {
            div(classes = "bg-green-500 h-full transition-all duration-300 ease-out") {
                id = "progressFill"
                style = "width: 0%"
            }
        }
        div(classes = "mt-3 text-gray-600") {
            id = "status"
            +"Ready to start"
        }
    }
}

private fun BODY.logContainer() {
    div(classes = "bg-gray-900 text-gray-300 p-4 rounded-lg max-h-96 overflow-y-auto font-mono text-sm") {
        id = "log"
    }
}

fun Application.geoJsonStreamRoute() {
    val geoBoundaryRepository by inject<GeoBoundaryRepository>()
    val geoAreaRepository by inject<GeoAreaRepository>()

    routing {
        sse("/geojson/{country}/stream") {
            val country = call.parameters["country"]
            if (country.isNullOrBlank()) {
                val errorEvent = GeoJsonProgressEvent.Error("Country parameter is required")
                send(ServerSentEvent(data = formatter.encodeToString(errorEvent), event = "progress"))
                return@sse
            }

            try {
                // Get ADM0 (country) polygon first
                val supportedRegion = SupportedRegion.all.find { it.code2 == country }
                val countryInfo = supportedRegion?.let {
                    GeoBoundaryRepository.CountryInfo(
                        name = it.nameEn,
                        nameEn = it.nameEn,
                        nameJa = null, // TODO: Add nameJa to SupportedRegion if needed
                        wikipedia = null,
                        thumbnailUrl = it.flagUrl,
                    )
                }
                val countryArea = geoBoundaryRepository.getCountryArea(country, countryInfo)

                // Get ADM1 regions
                val regions = geoBoundaryRepository.getEnrichedCountries(country)

                // Total = 1 (country) + ADM1 regions
                val startedEvent = GeoJsonProgressEvent.Started(totalRegions = regions.size + 1)
                send(ServerSentEvent(data = formatter.encodeToString(startedEvent), event = "progress"))

                var successCount = 0
                var failCount = 0

                // First, upsert the country (ADM0)
                runCatching { geoAreaRepository.upsertArea(countryArea) }
                    .onSuccess { countryId ->
                        successCount++
                        val event = GeoJsonProgressEvent.RegionCompleted(
                            index = 0,
                            regionName = countryArea.name,
                            success = true,
                        )
                        send(ServerSentEvent(data = formatter.encodeToString(event), event = "progress"))

                        // Process ADM1 regions
                        geoBoundaryRepository.getEnrichedAllAdminsAsFlow(country, regions).collect { (index, result) ->
                            result
                                .onSuccess { adm1GeoArea ->
                                    // Set parent to country
                                    val areaWithParent = adm1GeoArea.copy(parentId = countryId)

                                    runCatching {
                                        // Upsert ADM1
                                        val adm1Id = geoAreaRepository.upsertArea(areaWithParent)

                                        // Upsert ADM2 children with ADM1 as parent
                                        if (adm1GeoArea.children.isNotEmpty()) {
                                            val adm2WithParent = adm1GeoArea.children.map { it.copy(parentId = adm1Id) }
                                            geoAreaRepository.upsertAreasBatch(adm2WithParent)
                                        }
                                    }
                                        .onSuccess {
                                            successCount++
                                            val event = GeoJsonProgressEvent.RegionCompleted(
                                                index = index + 1, // +1 because country is index 0
                                                regionName = adm1GeoArea.name,
                                                success = true,
                                            )
                                            send(ServerSentEvent(data = formatter.encodeToString(event), event = "progress"))
                                        }
                                        .onFailure { e ->
                                            failCount++
                                            val event = GeoJsonProgressEvent.RegionCompleted(
                                                index = index + 1,
                                                regionName = adm1GeoArea.name,
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
                                        index = index + 1,
                                        regionName = regionName,
                                        success = false,
                                        errorMessage = e.message,
                                    )
                                    send(ServerSentEvent(data = formatter.encodeToString(event), event = "progress"))
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
                        )
                        send(ServerSentEvent(data = formatter.encodeToString(event), event = "progress"))
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
