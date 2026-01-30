package ui.enrichment

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
import model.MissingNameArea
import ui.common.PageStyles
import ui.common.countryCardWithProgress
import ui.common.logContainer
import ui.common.statisticsCard
import ui.common.tailwindCdn

/**
 * 名前補完 国選択ページ
 */
fun HTML.geoNameEnrichmentListPage() {
    head {
        meta(charset = "UTF-8")
        title("Geo Name Enrichment")
        tailwindCdn()
    }
    body(classes = "max-w-4xl mx-auto py-10 px-5 font-sans bg-gray-100 min-h-screen") {
        h1(classes = "text-gray-800 text-2xl font-bold mb-4") { +"Geo Name Enrichment" }
        p(classes = "text-gray-600 mb-6") { +"Select a country to enrich geo area names using Gemini AI:" }
        countrySelectionGrid()
    }
}

/**
 * 名前補完 進捗ページ（単一国）
 */
fun HTML.geoNameEnrichmentProgressPage(country: String, level: Int?) {
    val levelLabel = level?.let { "Level $it" } ?: "All Levels"
    head {
        meta(charset = "UTF-8")
        title("Geo Name Enrichment - $country")
        tailwindCdn()
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

/**
 * 名前補完 全国処理進捗ページ
 */
fun HTML.geoNamesAllCountriesProgressPage() {
    val totalRegions = SupportedRegion.all.sumOf { it.subRegionCount }
    head {
        meta(charset = "UTF-8")
        title("Geo Name Enrichment - All Countries")
        tailwindCdn()
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
                        countryCardWithProgress(index, region, "bg-purple-400")
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
                statisticsCard("totalStat", "Total Countries", "${SupportedRegion.all.size}", "bg-blue-500")
                statisticsCard("successStat", "Success", "0", "bg-green-500")
                statisticsCard("failedStat", "Failed", "0", "bg-red-500")
                statisticsCard("timeStat", "Elapsed Time", "00:00", "bg-purple-500")
            }

            // Log container
            logContainer()
            script(src = "/static/js/multi-country-progress.js") {}
        }
    }
}

/**
 * 欠落名称テーブルページ
 */
fun HTML.missingNamesPage(country: String, levelLabel: String, areas: List<MissingNameArea>) {
    head {
        meta(charset = "UTF-8")
        title("Missing Names - $country")
        tailwindCdn()
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

// ============= Private Helper Functions =============

private fun HEAD.enrichmentPageStyles() {
    style {
        unsafe {
            raw(
                listOf(
                    PageStyles.logStyles,
                    PageStyles.enrichmentStatusStyles,
                    PageStyles.animationStyles,
                ).joinToString("\n\n"),
            )
        }
    }
}

private fun HEAD.geoNamesAllCountriesPageStyles() {
    style {
        unsafe {
            raw(
                listOf(
                    PageStyles.logStyles,
                    PageStyles.enrichmentCountryCardStateStyles,
                    PageStyles.animationStyles,
                ).joinToString("\n\n"),
            )
        }
    }
}

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
                option {
                    attributes["value"] = "5"
                    +"5"
                }
                option {
                    attributes["value"] = "10"
                    attributes["selected"] = "true"
                    +"10"
                }
                option {
                    attributes["value"] = "20"
                    +"20"
                }
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

private fun DIV.missingNamesTable(areas: List<MissingNameArea>) {
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

@Suppress("UnusedParameter")
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
                option {
                    attributes["value"] = "5"
                    +"5"
                }
                option {
                    attributes["value"] = "10"
                    attributes["selected"] = "true"
                    +"10"
                }
                option {
                    attributes["value"] = "20"
                    +"20"
                }
            }
        }
    }
}
