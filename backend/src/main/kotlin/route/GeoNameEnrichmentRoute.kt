package route

import formatter
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.html.respondHtml
import io.ktor.server.routing.get
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
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.label
import kotlinx.html.meta
import kotlinx.html.onClick
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.html.unsafe
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.gemini.GeoNameEnrichmentEvent
import me.matsumo.travelog.core.model.geo.MultiCountryProgressEvent
import me.matsumo.travelog.core.repository.GeoNameEnrichmentRepository
import org.koin.ktor.ext.inject

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
                    head {
                        meta(charset = "UTF-8")
                        title("Missing Names - $country")
                        tailwindCdnForEnrichment()
                    }
                    body(classes = "bg-gray-100 min-h-screen") {
                        div(classes = "max-w-6xl mx-auto py-10 px-5") {
                            h1(classes = "text-2xl font-bold text-gray-800 mb-4") {
                                +"Missing Names: $country ($levelLabel)"
                            }
                            p(classes = "text-gray-600 mb-6") {
                                +"Found ${areas.size} areas with missing names"
                            }
                            missingNamesTable(areas)
                        }
                    }
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

// Page Components
private fun HTML.geoNameEnrichmentListPage() {
    head {
        meta(charset = "UTF-8")
        title("Geo Name Enrichment")
        tailwindCdnForEnrichment()
    }
    body(classes = "max-w-4xl mx-auto py-10 px-5 font-sans bg-gray-100 min-h-screen") {
        h1(classes = "text-gray-800 text-2xl font-bold mb-4") { +"Geo Name Enrichment" }
        p(classes = "text-gray-600 mb-6") { +"Select a country to enrich geo area names using Gemini AI:" }
        countrySelectionGrid()
    }
}

private fun HTML.geoNameEnrichmentProgressPage(country: String, level: Int?) {
    val levelLabel = level?.let { "Level $it" } ?: "All Levels"
    head {
        meta(charset = "UTF-8")
        title("Geo Name Enrichment - $country")
        tailwindCdnForEnrichment()
        enrichmentPageStyles()
    }
    body(classes = "min-h-screen bg-gray-100") {
        div(classes = "max-w-6xl mx-auto py-10 px-5") {
            h1(classes = "text-gray-800 text-2xl font-bold mb-4") { +"Geo Name Enrichment: $country ($levelLabel)" }
            enrichmentControlButtons(country, level)
            enrichmentStatisticsBar()
            enrichmentProgressContainer()
            enrichmentResultsTable()
            enrichmentLogContainer()
            enrichmentScript(country, level)
        }
    }
}

// Style Components
private fun HEAD.tailwindCdnForEnrichment() {
    script(src = "https://cdn.tailwindcss.com") {}
}

private fun HEAD.enrichmentPageStyles() {
    style {
        unsafe {
            raw(
                """
                .log-entry.success { color: #4CAF50; }
                .log-entry.error { color: #f44336; }
                .log-entry.info { color: #2196F3; }
                .log-entry.warning { color: #FF9800; }

                .status-applied { background-color: #10B981; color: white; }
                .status-validated { background-color: #3B82F6; color: white; }
                .status-skipped { background-color: #F59E0B; color: white; }
                .status-error { background-color: #EF4444; color: white; }

                .progress-fill {
                    transition: width 0.3s ease-out;
                }

                .confidence-high { color: #10B981; }
                .confidence-medium { color: #F59E0B; }
                .confidence-low { color: #EF4444; }

                .log-collapsed .log-content { display: none; }
                .log-toggle-icon { transition: transform 0.2s; }
                .log-collapsed .log-toggle-icon { transform: rotate(-90deg); }
                """.trimIndent(),
            )
        }
    }
}

// UI Components
private fun BODY.countrySelectionGrid() {
    div(classes = "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4") {
        geoNamesAllCountriesCard("/geo-names/enrich/all")
        SupportedRegion.all.forEach { region ->
            countryCard(region)
        }
    }
}

private fun DIV.geoNamesAllCountriesCard(href: String) {
    val totalRegions = SupportedRegion.all.sumOf { it.subRegionCount }
    a(
        href = href,
        classes = "bg-gradient-to-r from-purple-500 to-pink-600 hover:from-purple-600 hover:to-pink-700 " +
                "rounded-lg p-4 no-underline text-white transition-all flex items-center gap-3 shadow-lg",
    ) {
        div(classes = "w-12 h-8 flex items-center justify-center text-2xl") {
            +"🌍"
        }
        div(classes = "flex-1") {
            div(classes = "font-semibold mb-1") { +"All Countries" }
            div(classes = "text-xs text-purple-100") { +"${SupportedRegion.all.size} countries, $totalRegions regions" }
        }
    }
}

private fun DIV.countryCard(region: SupportedRegion) {
    a(
        href = "/geo-names/enrich/${region.code2}",
        classes = "bg-white hover:bg-gray-50 rounded-lg p-4 no-underline text-gray-800 transition-colors flex items-center gap-3 shadow-sm",
    ) {
        img(src = region.flagUrl, classes = "w-12 h-8 object-cover rounded") {
            attributes["alt"] = region.nameEn
        }
        div(classes = "flex-1") {
            div(classes = "font-semibold mb-1") { +region.nameEn }
            div(classes = "text-xs text-gray-500") { +"${region.subRegionCount} regions" }
        }
    }
}

private fun DIV.enrichmentControlButtons(country: String, level: Int?) {
    val levelParam = level?.toString() ?: "null"
    div(classes = "flex gap-3 mb-5 items-center") {
        button(
            classes = "bg-green-500 hover:bg-green-600 text-white font-semibold py-2 px-6 rounded-lg " +
                    "transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed",
        ) {
            id = "startBtn"
            onClick = "startEnrichment('$country', $levelParam, false)"
            +"Start"
        }
        button(
            classes = "bg-blue-500 hover:bg-blue-600 text-white font-semibold py-2 px-6 rounded-lg " +
                    "transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed",
        ) {
            id = "dryRunBtn"
            onClick = "startEnrichment('$country', $levelParam, true)"
            +"Dry Run"
        }
        button(
            classes = "bg-red-500 hover:bg-red-600 text-white font-semibold py-2 px-6 rounded-lg " +
                    "transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed",
        ) {
            id = "stopBtn"
            onClick = "stopEnrichment()"
            attributes["disabled"] = "true"
            +"Stop"
        }
        div(classes = "ml-4 flex items-center gap-2") {
            label(classes = "text-gray-600 text-sm") { +"Batch Size:" }
            select(classes = "border rounded px-2 py-1") {
                id = "batchSize"
                option { attributes["value"] = "5"; +"5" }
                option { attributes["value"] = "10"; attributes["selected"] = "true"; +"10" }
                option { attributes["value"] = "20"; +"20" }
            }
        }
    }
}

private fun DIV.enrichmentStatisticsBar() {
    div(classes = "grid grid-cols-5 gap-4 mb-5") {
        enrichmentStatCard("totalStat", "Total", "0", "bg-blue-500")
        enrichmentStatCard("appliedStat", "Applied", "0", "bg-green-500")
        enrichmentStatCard("validatedStat", "Validated", "0", "bg-blue-400")
        enrichmentStatCard("skippedStat", "Skipped", "0", "bg-yellow-500")
        enrichmentStatCard("failedStat", "Failed", "0", "bg-red-500")
    }
}

private fun DIV.enrichmentStatCard(cardId: String, label: String, value: String, colorClass: String) {
    div(classes = "bg-white rounded-lg p-4 shadow-sm") {
        id = cardId
        div(classes = "flex items-center gap-3") {
            div(classes = "$colorClass w-10 h-10 rounded-full flex items-center justify-center") {
                span(classes = "text-white text-lg font-bold") {
                    id = "${cardId}Value"
                    +value
                }
            }
            div {
                div(classes = "text-xs text-gray-500 uppercase tracking-wide") { +label }
                div(classes = "text-xl font-bold text-gray-800") {
                    id = "${cardId}Display"
                    +value
                }
            }
        }
    }
}

private fun DIV.enrichmentProgressContainer() {
    div(classes = "bg-white rounded-lg p-5 mb-5 shadow-sm") {
        div(classes = "bg-gray-200 rounded-full h-4 overflow-hidden") {
            div(classes = "bg-gradient-to-r from-blue-500 to-green-500 h-full progress-fill") {
                id = "progressFill"
                style = "width: 0%"
            }
        }
        div(classes = "mt-3 text-gray-600 text-sm") {
            id = "status"
            +"Ready to start"
        }
    }
}

private fun DIV.enrichmentResultsTable() {
    div(classes = "bg-white rounded-lg shadow-sm mb-5 overflow-hidden") {
        h3(classes = "text-lg font-semibold text-gray-700 p-4 border-b") { +"Results" }
        div(classes = "overflow-x-auto max-h-96 overflow-y-auto") {
            table(classes = "w-full text-sm") {
                thead(classes = "bg-gray-50 sticky top-0") {
                    tr {
                        th(classes = "px-4 py-2 text-left") { +"Original" }
                        th(classes = "px-4 py-2 text-left") { +"English" }
                        th(classes = "px-4 py-2 text-left") { +"Japanese" }
                        th(classes = "px-4 py-2 text-center") { +"Confidence" }
                        th(classes = "px-4 py-2 text-center") { +"Status" }
                    }
                }
                tbody {
                    id = "resultsBody"
                }
            }
        }
    }
}

private fun DIV.enrichmentLogContainer() {
    div(classes = "bg-white rounded-lg shadow-sm") {
        id = "logWrapper"
        div(classes = "flex items-center justify-between p-4 cursor-pointer select-none border-b border-gray-200") {
            id = "logHeader"
            onClick = "toggleLog()"
            div(classes = "flex items-center gap-2") {
                span(classes = "log-toggle-icon text-gray-500") { +"▼" }
                h3(classes = "text-lg font-semibold text-gray-700") { +"Logs" }
                span(classes = "text-xs text-gray-400 ml-2") {
                    id = "logCount"
                    +"0 entries"
                }
            }
            span(classes = "text-xs text-gray-400") { +"Click to toggle" }
        }
        div(classes = "log-content") {
            div(classes = "bg-gray-900 text-gray-300 p-4 max-h-64 overflow-y-auto font-mono text-xs rounded-b-lg") {
                id = "log"
            }
        }
    }
}

private fun DIV.missingNamesTable(areas: List<me.matsumo.travelog.core.model.gemini.MissingNameArea>) {
    div(classes = "bg-white rounded-lg shadow-sm overflow-hidden") {
        div(classes = "overflow-x-auto") {
            table(classes = "w-full text-sm") {
                thead(classes = "bg-gray-50") {
                    tr {
                        th(classes = "px-4 py-2 text-left") { +"Name" }
                        th(classes = "px-4 py-2 text-left") { +"Name (EN)" }
                        th(classes = "px-4 py-2 text-left") { +"Name (JA)" }
                        th(classes = "px-4 py-2 text-left") { +"Parent" }
                        th(classes = "px-4 py-2 text-left") { +"ADM ID" }
                    }
                }
                tbody {
                    areas.forEach { area ->
                        tr(classes = "border-t hover:bg-gray-50") {
                            td(classes = "px-4 py-2") { +area.name }
                            td(classes = "px-4 py-2 ${if (area.nameEn == null) "text-red-500" else ""}") {
                                +(area.nameEn ?: "Missing")
                            }
                            td(classes = "px-4 py-2 ${if (area.nameJa == null) "text-red-500" else ""}") {
                                +(area.nameJa ?: "Missing")
                            }
                            td(classes = "px-4 py-2 text-gray-500") { +(area.parentName ?: "-") }
                            td(classes = "px-4 py-2 text-gray-400 text-xs") { +area.admId }
                        }
                    }
                }
            }
        }
    }
}

private fun DIV.enrichmentScript(country: String, level: Int?) {
    script {
        unsafe {
            raw(
                """
                let eventSource = null;
                let logEntries = 0;
                const startTime = Date.now();

                function startEnrichment(country, level, dryRun) {
                    const batchSize = document.getElementById('batchSize').value;
                    document.getElementById('startBtn').disabled = true;
                    document.getElementById('dryRunBtn').disabled = true;
                    document.getElementById('stopBtn').disabled = false;
                    document.getElementById('resultsBody').innerHTML = '';
                    logEntries = 0;

                    let url = '/geo-names/enrich/' + country + '/stream?dryRun=' + dryRun + '&batchSize=' + batchSize;
                    if (level !== null) {
                        url += '&level=' + level;
                    }
                    eventSource = new EventSource(url);

                    eventSource.addEventListener('progress', function(event) {
                        const data = JSON.parse(event.data);
                        handleEvent(data, dryRun);
                    });

                    eventSource.onerror = function(error) {
                        console.error('EventSource error:', error);
                        addLog('Connection error', 'error');
                        stopEnrichment();
                    };

                    updateStatus('Processing...');
                    addLog('Started ' + (dryRun ? '(dry run)' : ''), 'info');
                }

                function stopEnrichment() {
                    if (eventSource) {
                        eventSource.close();
                        eventSource = null;
                    }
                    document.getElementById('startBtn').disabled = false;
                    document.getElementById('dryRunBtn').disabled = false;
                    document.getElementById('stopBtn').disabled = true;
                }

                function handleEvent(data, dryRun) {
                    switch(data.type) {
                        case 'started':
                            updateStatus('Processing ' + data.totalCount + ' areas...');
                            document.getElementById('totalStatDisplay').textContent = data.totalCount;
                            document.getElementById('totalStatValue').textContent = data.totalCount;
                            addLog('Found ' + data.totalCount + ' areas to process', 'info');
                            break;

                        case 'batch_processed':
                            const progress = (data.batchIndex / data.totalBatches) * 100;
                            document.getElementById('progressFill').style.width = progress + '%';
                            document.getElementById('appliedStatDisplay').textContent = data.appliedCount;
                            document.getElementById('appliedStatValue').textContent = data.appliedCount;
                            document.getElementById('validatedStatDisplay').textContent = data.validatedCount;
                            document.getElementById('validatedStatValue').textContent = data.validatedCount;
                            document.getElementById('skippedStatDisplay').textContent = data.skippedCount;
                            document.getElementById('skippedStatValue').textContent = data.skippedCount;
                            updateStatus('Batch ' + data.batchIndex + '/' + data.totalBatches + ' completed');
                            break;

                        case 'item_result':
                            addResultRow(data);
                            break;

                        case 'completed':
                            document.getElementById('progressFill').style.width = '100%';
                            updateStatus('Completed! ' + data.successCount + ' success, ' + data.failedCount + ' failed' + (dryRun ? ' (dry run)' : ''));
                            addLog('Completed in ' + (data.elapsedMs / 1000).toFixed(1) + 's', 'success');
                            stopEnrichment();
                            break;

                        case 'error':
                            addLog('Error: ' + data.message, 'error');
                            document.getElementById('failedStatDisplay').textContent =
                                parseInt(document.getElementById('failedStatDisplay').textContent) + 1;
                            break;
                    }
                }

                function addResultRow(data) {
                    const tbody = document.getElementById('resultsBody');
                    const row = document.createElement('tr');
                    row.className = 'border-t hover:bg-gray-50';

                    const confidenceClass = data.confidence >= 0.8 ? 'confidence-high' :
                                           data.confidence >= 0.5 ? 'confidence-medium' : 'confidence-low';
                    const statusClass = 'status-' + data.status.toLowerCase();

                    row.innerHTML =
                        '<td class="px-4 py-2">' + data.originalName + '</td>' +
                        '<td class="px-4 py-2">' + (data.nameEn || '-') + '</td>' +
                        '<td class="px-4 py-2">' + (data.nameJa || '-') + '</td>' +
                        '<td class="px-4 py-2 text-center ' + confidenceClass + '">' + (data.confidence * 100).toFixed(0) + '%</td>' +
                        '<td class="px-4 py-2 text-center"><span class="px-2 py-1 rounded text-xs ' + statusClass + '">' + data.status + '</span></td>';

                    tbody.appendChild(row);
                    tbody.scrollTop = tbody.scrollHeight;
                }

                function updateStatus(message) {
                    document.getElementById('status').textContent = message;
                }

                function addLog(message, type) {
                    const log = document.getElementById('log');
                    const entry = document.createElement('div');
                    entry.className = 'log-entry ' + type;
                    const timestamp = new Date().toLocaleTimeString();
                    entry.textContent = '[' + timestamp + '] ' + message;
                    log.appendChild(entry);
                    log.scrollTop = log.scrollHeight;
                    logEntries++;
                    document.getElementById('logCount').textContent = logEntries + ' entries';
                }

                function toggleLog() {
                    document.getElementById('logWrapper').classList.toggle('log-collapsed');
                }
                """.trimIndent(),
            )
        }
    }
}

// ============= All Countries Processing =============

/**
 * 全国名前補完処理進捗ページ
 */
private fun HTML.geoNamesAllCountriesProgressPage() {
    val totalRegions = SupportedRegion.all.sumOf { it.subRegionCount }
    head {
        meta(charset = "UTF-8")
        title("Geo Name Enrichment - All Countries")
        tailwindCdnForEnrichment()
        geoNamesAllCountriesPageStyles()
    }
    body(classes = "min-h-screen bg-gray-100") {
        div(classes = "max-w-6xl mx-auto py-10 px-5") {
            h1(classes = "text-gray-800 text-2xl font-bold mb-4") {
                +"Geo Name Enrichment: All Countries"
            }
            p(classes = "text-gray-600 mb-6") {
                +"Enriching names for ${SupportedRegion.all.size} countries with $totalRegions regions total"
            }

            // Control buttons
            geoNamesAllCountriesControlButtons()

            // Overall progress
            div(classes = "bg-white rounded-lg p-5 mb-5 shadow-sm") {
                div(classes = "flex justify-between items-center mb-2") {
                    span(classes = "text-gray-700 font-semibold") { +"Overall Progress" }
                    span(classes = "text-gray-500 text-sm") {
                        id = "overallProgressText"
                        +"0 / ${SupportedRegion.all.size} countries"
                    }
                }
                div(classes = "bg-gray-200 rounded-full h-4 overflow-hidden") {
                    div(classes = "bg-gradient-to-r from-purple-500 to-pink-600 h-full progress-fill") {
                        id = "overallProgressFill"
                        style = "width: 0%"
                    }
                }
                div(classes = "mt-3 text-gray-600 text-sm") {
                    id = "overallStatus"
                    +"Ready to start"
                }
            }

            // Country cards grid
            div(classes = "mb-5") {
                h3(classes = "text-lg font-semibold text-gray-700 mb-3") { +"Countries" }
                div(classes = "grid grid-cols-2 md:grid-cols-4 gap-3") {
                    id = "countryCards"
                    SupportedRegion.all.forEachIndexed { index, region ->
                        geoNamesAllCountriesCountryCard(index, region)
                    }
                }
            }

            // Current country detail section (initially hidden)
            div(classes = "hidden") {
                id = "currentCountrySection"
                div(classes = "bg-white rounded-lg p-5 mb-5 shadow-sm") {
                    h3(classes = "text-lg font-semibold text-gray-700 mb-3") {
                        +"Current: "
                        span {
                            id = "currentCountryName"
                            +""
                        }
                    }
                    // Inner progress bar
                    div(classes = "bg-gray-200 rounded-full h-3 overflow-hidden mb-3") {
                        div(classes = "bg-gradient-to-r from-purple-400 to-pink-500 h-full progress-fill") {
                            id = "currentCountryProgressFill"
                            style = "width: 0%"
                        }
                    }
                    div(classes = "text-gray-600 text-sm") {
                        id = "currentCountryStatus"
                        +""
                    }
                }
            }

            // Statistics
            div(classes = "grid grid-cols-4 gap-4 mb-5") {
                geoNamesAllCountriesStatCard("totalStat", "Total Countries", "${SupportedRegion.all.size}", "bg-blue-500")
                geoNamesAllCountriesStatCard("successStat", "Success", "0", "bg-green-500")
                geoNamesAllCountriesStatCard("failedStat", "Failed", "0", "bg-red-500")
                geoNamesAllCountriesStatCard("timeStat", "Elapsed Time", "00:00", "bg-purple-500")
            }

            // Log container
            geoNamesAllCountriesLogContainer()
            script(src = "/static/js/multi-country-progress.js") {}
        }
    }
}

private fun HEAD.geoNamesAllCountriesPageStyles() {
    style {
        unsafe {
            raw(
                """
                .log-entry.success { color: #4CAF50; }
                .log-entry.error { color: #f44336; }
                .log-entry.info { color: #2196F3; }
                .log-entry.warning { color: #FF9800; }

                /* Country card states */
                .country-card[data-state="pending"] {
                    opacity: 0.6;
                }
                .country-card[data-state="processing"] {
                    border: 2px solid #A855F7;
                    box-shadow: 0 0 10px rgba(168, 85, 247, 0.3);
                    opacity: 1;
                }
                .country-card[data-state="completed"] {
                    border: 2px solid #10B981;
                    opacity: 1;
                }
                .country-card[data-state="error"] {
                    border: 2px solid #EF4444;
                    opacity: 1;
                }

                @keyframes pulse {
                    0%, 100% { opacity: 1; }
                    50% { opacity: 0.5; }
                }
                .animate-pulse { animation: pulse 2s infinite; }

                .progress-fill {
                    transition: width 0.3s ease-out;
                }

                .log-collapsed .log-content { display: none; }
                .log-toggle-icon { transition: transform 0.2s; }
                .log-collapsed .log-toggle-icon { transform: rotate(-90deg); }
                """.trimIndent(),
            )
        }
    }
}

private fun DIV.geoNamesAllCountriesControlButtons() {
    div(classes = "flex gap-3 mb-5 items-center flex-wrap") {
        button(
            classes = "bg-green-500 hover:bg-green-600 text-white font-semibold py-2 px-6 rounded-lg " +
                    "transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed",
        ) {
            id = "startBtn"
            onClick = "startAllCountriesProcessing('geo-names')"
            +"Start"
        }
        button(
            classes = "bg-red-500 hover:bg-red-600 text-white font-semibold py-2 px-6 rounded-lg " +
                    "transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed",
        ) {
            id = "stopBtn"
            onClick = "stopAllCountriesProcessing()"
            attributes["disabled"] = "true"
            +"Stop"
        }

        // Batch size selector
        div(classes = "ml-4 flex items-center gap-2") {
            label(classes = "text-gray-600 text-sm") { +"Batch Size:" }
            select(classes = "border rounded px-2 py-1 text-sm") {
                id = "batchSize"
                option { attributes["value"] = "5"; +"5" }
                option { attributes["value"] = "10"; attributes["selected"] = "true"; +"10" }
                option { attributes["value"] = "20"; +"20" }
            }
        }
    }
}

private fun DIV.geoNamesAllCountriesStatCard(cardId: String, label: String, value: String, colorClass: String) {
    div(classes = "bg-white rounded-lg p-4 shadow-sm") {
        id = cardId
        div(classes = "flex items-center gap-3") {
            div(classes = "$colorClass w-10 h-10 rounded-full flex items-center justify-center") {
                span(classes = "text-white text-lg font-bold") {
                    id = "${cardId}Value"
                    +value
                }
            }
            div {
                div(classes = "text-xs text-gray-500 uppercase tracking-wide") { +label }
                div(classes = "text-xl font-bold text-gray-800") {
                    id = "${cardId}Display"
                    +value
                }
            }
        }
    }
}

private fun DIV.geoNamesAllCountriesCountryCard(index: Int, region: SupportedRegion) {
    div(classes = "country-card bg-white rounded-lg p-3 shadow-sm border-2 border-transparent") {
        id = "country-card-$index"
        attributes["data-state"] = "pending"
        attributes["data-code"] = region.code2

        div(classes = "flex items-center gap-2") {
            img(
                src = region.flagUrl,
                alt = region.nameEn,
                classes = "w-8 h-6 object-cover rounded",
            )
            div(classes = "flex-1 min-w-0") {
                div(classes = "font-semibold text-gray-800 text-sm truncate") { +region.nameEn }
                div(classes = "text-xs text-gray-500") { +"${region.subRegionCount} regions" }
            }
            // Status indicator
            span(classes = "status-icon text-lg hidden") {
                id = "country-status-$index"
            }
        }
        // Inner progress bar (hidden initially)
        div(classes = "mt-2 hidden") {
            id = "country-progress-$index"
            div(classes = "bg-gray-200 rounded-full h-1.5 overflow-hidden") {
                div(classes = "bg-purple-400 h-full progress-fill") {
                    id = "country-progress-bar-$index"
                    style = "width: 0%"
                }
            }
            div(classes = "text-xs text-gray-500 mt-1") {
                id = "country-progress-text-$index"
                +"0/0"
            }
        }
    }
}

private fun DIV.geoNamesAllCountriesLogContainer() {
    div(classes = "bg-white rounded-lg shadow-sm") {
        id = "logWrapper"
        // Log header (clickable to toggle)
        div(classes = "flex items-center justify-between p-4 cursor-pointer select-none border-b border-gray-200") {
            id = "logHeader"
            onClick = "toggleLog()"
            div(classes = "flex items-center gap-2") {
                span(classes = "log-toggle-icon text-gray-500") { +"▼" }
                h3(classes = "text-lg font-semibold text-gray-700") { +"Logs" }
                span(classes = "text-xs text-gray-400 ml-2") {
                    id = "logCount"
                    +"0 entries"
                }
            }
            span(classes = "text-xs text-gray-400") { +"Click to toggle" }
        }
        // Log content
        div(classes = "log-content") {
            div(classes = "bg-gray-900 text-gray-300 p-4 max-h-64 overflow-y-auto font-mono text-xs rounded-b-lg") {
                id = "log"
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
