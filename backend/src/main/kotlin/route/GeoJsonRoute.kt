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
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import me.matsumo.travelog.core.model.geo.GeoJsonProgressEvent
import me.matsumo.travelog.core.repository.GeoBoundaryRepository
import me.matsumo.travelog.core.repository.GeoRegionRepository
import org.koin.ktor.ext.inject

private data class RegionInfo(
    val code: String,
    val name: String,
    val subRegionCount: Int,
)

private val supportedRegions = listOf(
    RegionInfo("JP", "Japan", 47),
    RegionInfo("KR", "Korea", 17),
    RegionInfo("TW", "Taiwan", 22),
    RegionInfo("CN", "China", 34),
    RegionInfo("US", "United States", 50),
    RegionInfo("GB", "United Kingdom", 4),
    RegionInfo("FR", "France", 18),
    RegionInfo("DE", "Germany", 16),
)

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
        commonStyles()
        regionListStyles()
    }
    body {
        h1 { +"GeoJSON Processing" }
        p { +"Select a region to process:" }
        regionList()
    }
}

private fun HTML.progressPage(country: String) {
    head {
        meta(charset = "UTF-8")
        title("GeoJSON Processing - $country")
        commonStyles()
        progressStyles()
    }
    body {
        h1 { +"GeoJSON Processing: $country" }
        progressContainer()
        logContainer()
        progressScript(country)
    }
}

// スタイルコンポーネント
private fun HEAD.commonStyles() {
    style {
        unsafe {
            raw(
                """
                body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; max-width: 800px; margin: 40px auto; padding: 0 20px; }
                h1 { color: #333; }
                """.trimIndent(),
            )
        }
    }
}

private fun HEAD.regionListStyles() {
    style {
        unsafe {
            raw(
                """
                .region-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; }
                .region-card { background: #f5f5f5; border-radius: 8px; padding: 16px; text-decoration: none; color: #333; transition: background 0.2s; display: flex; align-items: center; gap: 12px; }
                .region-card:hover { background: #e0e0e0; }
                .flag { width: 48px; height: 36px; object-fit: cover; border-radius: 4px; }
                .region-info { flex: 1; }
                .region-name { font-weight: 600; margin-bottom: 4px; }
                .region-count { font-size: 12px; color: #666; }
                """.trimIndent(),
            )
        }
    }
}

private fun HEAD.progressStyles() {
    style {
        unsafe {
            raw(
                """
                .progress-container { background: #f0f0f0; border-radius: 8px; padding: 20px; margin: 20px 0; }
                .progress-bar { background: #e0e0e0; border-radius: 4px; height: 24px; overflow: hidden; }
                .progress-fill { background: #4CAF50; height: 100%; transition: width 0.3s ease; }
                .status { margin-top: 10px; color: #666; }
                .log { background: #1e1e1e; color: #d4d4d4; padding: 16px; border-radius: 8px; max-height: 400px; overflow-y: auto; font-family: monospace; font-size: 13px; }
                .log-entry { margin: 4px 0; }
                .success { color: #4CAF50; }
                .error { color: #f44336; }
                .info { color: #2196F3; }
                """.trimIndent(),
            )
        }
    }
}

// UI コンポーネント
private fun BODY.regionList() {
    div(classes = "region-list") {
        supportedRegions.forEach { region ->
            regionCard(region)
        }
    }
}

private fun DIV.regionCard(region: RegionInfo) {
    a(href = "/geojson/${region.code}", classes = "region-card") {
        img(
            src = "https://flagcdn.com/h240/${region.code.lowercase()}.webp",
            alt = region.name,
            classes = "flag",
        )
        div(classes = "region-info") {
            div(classes = "region-name") { +region.name }
            div(classes = "region-count") { +"${region.subRegionCount} regions" }
        }
    }
}

private fun BODY.progressContainer() {
    div(classes = "progress-container") {
        div(classes = "progress-bar") {
            div(classes = "progress-fill") {
                id = "progressFill"
                style = "width: 0%"
            }
        }
        div(classes = "status") {
            id = "status"
            +"Connecting..."
        }
    }
}

private fun BODY.logContainer() {
    div(classes = "log") {
        id = "log"
    }
}

private fun BODY.progressScript(country: String) {
    script {
        unsafe {
            raw(
                """
                const eventSource = new EventSource('/geojson/$country/stream');
                const log = document.getElementById('log');
                const progressFill = document.getElementById('progressFill');
                const status = document.getElementById('status');
                let totalRegions = 0;
                let processed = 0;

                function addLog(message, type = 'info') {
                    const entry = document.createElement('div');
                    entry.className = 'log-entry ' + type;
                    entry.textContent = new Date().toLocaleTimeString() + ' - ' + message;
                    log.appendChild(entry);
                    log.scrollTop = log.scrollHeight;
                }

                eventSource.addEventListener('progress', function(e) {
                    const data = JSON.parse(e.data);

                    if (data.type === 'started') {
                        totalRegions = data.totalRegions;
                        status.textContent = 'Processing 0 / ' + totalRegions + ' regions...';
                        addLog('Started processing ' + totalRegions + ' regions', 'info');
                    } else if (data.type === 'region_completed') {
                        processed++;
                        const percent = Math.round((processed / totalRegions) * 100);
                        progressFill.style.width = percent + '%';
                        status.textContent = 'Processing ' + processed + ' / ' + totalRegions + ' regions...';
                        if (data.success) {
                            addLog('[' + (data.index + 1) + '/' + totalRegions + '] ' + data.regionName + ' - OK', 'success');
                        } else {
                            addLog('[' + (data.index + 1) + '/' + totalRegions + '] ' + data.regionName + ' - Failed: ' + (data.errorMessage || 'Unknown error'), 'error');
                        }
                    } else if (data.type === 'completed') {
                        progressFill.style.width = '100%';
                        status.textContent = 'Completed! Success: ' + data.successCount + ', Failed: ' + data.failCount;
                        addLog('Processing completed. Success: ' + data.successCount + ', Failed: ' + data.failCount, data.failCount > 0 ? 'error' : 'success');
                        eventSource.close();
                    } else if (data.type === 'error') {
                        status.textContent = 'Error: ' + data.message;
                        addLog('Error: ' + data.message, 'error');
                        eventSource.close();
                    }
                });

                eventSource.onerror = function() {
                    status.textContent = 'Connection lost';
                    addLog('Connection to server lost', 'error');
                };
                """.trimIndent(),
            )
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
