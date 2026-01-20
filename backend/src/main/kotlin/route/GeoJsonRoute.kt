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
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.input
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
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.html.unsafe
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.model.geo.GeoJsonProgressEvent
import me.matsumo.travelog.core.repository.Adm1ProcessingEvent
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.GeoBoundaryMapper
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
        progressPageStyles()
    }
    body(classes = "min-h-screen bg-gray-100") {
        div(classes = "max-w-6xl mx-auto py-10 px-5") {
            h1(classes = "text-gray-800 text-2xl font-bold mb-4") { +"GeoJSON Processing: $country" }
            extendedControlButtons(country)

            // Phase 1: GeoJSON Processing (既存)
            div {
                id = "geojsonSection"
                statisticsBar()
                progressContainer()
                regionCardsGrid()
            }

            // Phase 2: Name Enrichment (新規、初期非表示)
            div(classes = "hidden") {
                id = "enrichmentSection"
                enrichmentSectionHeader()
                enrichmentStatisticsBar()
                enrichmentProgressContainer()
                enrichmentResultsTable()
            }

            // 完了後プロンプト (初期非表示)
            div(classes = "hidden") {
                id = "enrichmentPrompt"
                enrichmentPromptCard(country)
            }

            logContainer()
            script(src = "/static/js/integrated-progress.js") {}
        }
    }
}

// スタイルコンポーネント
private fun HEAD.tailwindCdn() {
    script(src = "https://cdn.tailwindcss.com") {}
}

private fun HEAD.progressPageStyles() {
    style {
        unsafe {
            raw(
                """
                .log-entry.success { color: #4CAF50; }
                .log-entry.error { color: #f44336; }
                .log-entry.info { color: #2196F3; }
                .log-entry.warning { color: #FF9800; }

                /* Card states */
                .region-card[data-state="pending"] {
                    border: 2px solid transparent;
                    opacity: 0.7;
                }
                .region-card[data-state="processing"] {
                    border: 2px solid #3B82F6;
                    box-shadow: 0 0 10px rgba(59, 130, 246, 0.3);
                }
                .region-card[data-state="completed"] {
                    border: 2px solid #10B981;
                }
                .region-card[data-state="error"] {
                    border: 2px solid #EF4444;
                }

                /* Pulse animation */
                @keyframes pulse {
                    0%, 100% { opacity: 1; }
                    50% { opacity: 0.5; }
                }
                .animate-pulse { animation: pulse 2s infinite; }

                /* Progress bar animation */
                .progress-fill {
                    transition: width 0.3s ease-out;
                }

                /* Collapsible log */
                .log-collapsed .log-content {
                    display: none;
                }
                .log-toggle-icon {
                    transition: transform 0.2s;
                }
                .log-collapsed .log-toggle-icon {
                    transform: rotate(-90deg);
                }

                /* Enrichment status badges */
                .status-applied { background-color: #10B981; color: white; }
                .status-validated { background-color: #3B82F6; color: white; }
                .status-skipped { background-color: #F59E0B; color: white; }
                .status-error { background-color: #EF4444; color: white; }

                /* Confidence colors */
                .confidence-high { color: #10B981; }
                .confidence-medium { color: #F59E0B; }
                .confidence-low { color: #EF4444; }
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
        classes = "bg-gray-100 hover:bg-gray-200 rounded-lg p-4 no-underline text-gray-800 transition-colors flex items-center gap-3",
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

private fun DIV.extendedControlButtons(country: String) {
    div(classes = "flex gap-3 mb-5 items-center flex-wrap") {
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

        // Auto-enrich checkbox
        label(classes = "flex items-center gap-2 text-gray-600 ml-4 cursor-pointer") {
            input(type = InputType.checkBox, classes = "w-4 h-4 rounded border-gray-300") {
                id = "autoEnrichCheckbox"
            }
            +"GeoJSON完了後に名前補完を実行"
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

private fun DIV.statisticsBar() {
    div(classes = "grid grid-cols-4 gap-4 mb-5") {
        statisticsCard("totalStat", "Total Regions", "0", "bg-blue-500")
        statisticsCard("successStat", "Success", "0", "bg-green-500")
        statisticsCard("failedStat", "Failed", "0", "bg-red-500")
        statisticsCard("timeStat", "Elapsed Time", "00:00", "bg-purple-500")
    }
}

private fun DIV.statisticsCard(cardId: String, label: String, value: String, colorClass: String) {
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

private fun DIV.progressContainer() {
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

private fun DIV.regionCardsGrid() {
    div(classes = "mb-5") {
        h3(classes = "text-lg font-semibold text-gray-700 mb-3") { +"Regions" }
        div(classes = "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4") {
            id = "regionCards"
            // Cards will be dynamically inserted by JavaScript
        }
    }
}

private fun DIV.logContainer() {
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

// ============= Enrichment UI Components =============

private fun DIV.enrichmentSectionHeader() {
    div(classes = "flex items-center gap-3 mb-4 mt-8") {
        h3(classes = "text-lg font-semibold text-gray-700") { +"Phase 2: Name Enrichment" }
        span(classes = "text-xs px-2 py-1 bg-blue-100 text-blue-600 rounded-full") { +"Gemini AI" }
    }
}

private fun DIV.enrichmentStatisticsBar() {
    div(classes = "grid grid-cols-5 gap-4 mb-5") {
        enrichmentStatCard("enrichmentTotalStat", "Total", "0", "bg-blue-500")
        enrichmentStatCard("enrichmentAppliedStat", "Applied", "0", "bg-green-500")
        enrichmentStatCard("enrichmentValidatedStat", "Validated", "0", "bg-blue-400")
        enrichmentStatCard("enrichmentSkippedStat", "Skipped", "0", "bg-yellow-500")
        enrichmentStatCard("enrichmentFailedStat", "Failed", "0", "bg-red-500")
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
            div(classes = "bg-gradient-to-r from-purple-500 to-pink-500 h-full progress-fill") {
                id = "enrichmentProgressFill"
                style = "width: 0%"
            }
        }
        div(classes = "mt-3 text-gray-600 text-sm") {
            id = "enrichmentStatus"
            +"Waiting for GeoJSON to complete..."
        }
    }
}

private fun DIV.enrichmentResultsTable() {
    div(classes = "bg-white rounded-lg shadow-sm mb-5 overflow-hidden") {
        h3(classes = "text-lg font-semibold text-gray-700 p-4 border-b") { +"Enrichment Results" }
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
                    id = "enrichmentResultsBody"
                }
            }
        }
    }
}

private fun DIV.enrichmentPromptCard(country: String) {
    div(classes = "bg-white rounded-lg p-6 shadow-sm mb-5 border-l-4 border-blue-500") {
        div(classes = "flex items-start gap-4") {
            div(classes = "bg-blue-100 p-3 rounded-full") {
                span(classes = "text-2xl") { +"✨" }
            }
            div(classes = "flex-1") {
                h3(classes = "text-lg font-semibold text-gray-800 mb-2") { +"GeoJSON Processing Complete!" }
                p(classes = "text-gray-600 mb-4") {
                    +"Would you like to enrich area names using Gemini AI? This will add English and Japanese names where missing."
                }
                div(classes = "flex gap-3") {
                    button(
                        classes = "bg-green-500 hover:bg-green-600 text-white font-semibold py-2 px-6 rounded-lg transition-colors",
                    ) {
                        onClick = "startEnrichmentOnly('$country', false)"
                        +"Start Enrichment"
                    }
                    button(
                        classes = "bg-blue-500 hover:bg-blue-600 text-white font-semibold py-2 px-6 rounded-lg transition-colors",
                    ) {
                        onClick = "startEnrichmentOnly('$country', true)"
                        +"Dry Run"
                    }
                    button(
                        classes = "bg-gray-400 hover:bg-gray-500 text-white font-semibold py-2 px-6 rounded-lg transition-colors",
                    ) {
                        onClick = "skipEnrichment()"
                        +"Skip"
                    }
                }
            }
        }
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

            val overallStartTime = System.currentTimeMillis()

            try {
                // Get ADM0 (country) polygon first
                val supportedRegion = SupportedRegion.all.find { it.code2 == country }
                val countryInfo = supportedRegion?.let {
                    GeoBoundaryRepository.CountryInfo(
                        name = it.nameEn,
                        nameEn = it.nameEn,
                        nameJa = null,
                        wikipedia = null,
                        thumbnailUrl = it.flagUrl,
                    )
                }
                val countryArea = geoBoundaryRepository.getCountryArea(country, countryInfo)

                // Get ADM1 regions
                val regions = geoBoundaryRepository.getEnrichedCountries(country)

                // Build RegionInfo list for initial card display
                val regionInfoList = buildRegionInfoList(supportedRegion, countryArea, regions)

                // Total = 1 (country) + ADM1 regions
                val totalRegions = regions.size + 1
                val startedEvent = GeoJsonProgressEvent.Started(
                    totalRegions = totalRegions,
                    regions = regionInfoList,
                )
                send(ServerSentEvent(data = formatter.encodeToString(startedEvent), event = "progress"))

                var successCount = 0
                var failCount = 0
                var totalAdm2Count = 0

                // First, upsert the country (ADM0)
                val adm0StartTime = System.currentTimeMillis()

                // Send region started event for ADM0
                val adm0StartedEvent = GeoJsonProgressEvent.RegionStarted(
                    index = 0,
                    regionName = countryArea.name,
                    level = 0,
                    adm2Count = 0,
                )
                send(ServerSentEvent(data = formatter.encodeToString(adm0StartedEvent), event = "progress"))

                runCatching { geoAreaRepository.upsertArea(countryArea) }
                    .onSuccess { countryId ->
                        successCount++
                        val event = GeoJsonProgressEvent.RegionCompleted(
                            index = 0,
                            regionName = countryArea.name,
                            success = true,
                            level = 0,
                            nameEn = countryArea.nameEn,
                            nameJa = countryArea.nameJa,
                            isoCode = countryArea.isoCode,
                            thumbnailUrl = supportedRegion?.flagUrl,
                            wikipedia = countryArea.wikipedia,
                            centerLat = countryArea.center?.lat,
                            centerLon = countryArea.center?.lon,
                            processingTimeMs = System.currentTimeMillis() - adm0StartTime,
                        )
                        send(ServerSentEvent(data = formatter.encodeToString(event), event = "progress"))

                        // Process ADM1 regions
                        geoBoundaryRepository.getEnrichedAllAdminsAsFlow(country, regions).collect { event ->
                            when (event) {
                                is Adm1ProcessingEvent.Started -> {
                                    // RegionStarted イベント送信のみ（DB処理なし）
                                    // permit取得時点で発火するため、UIは即座に「Processing...」に変わる
                                    val regionStartedEvent = GeoJsonProgressEvent.RegionStarted(
                                        index = event.index + 1,
                                        regionName = event.regionName,
                                        level = 1,
                                        adm2Count = event.adm2Count,
                                    )
                                    send(ServerSentEvent(data = formatter.encodeToString(regionStartedEvent), event = "progress"))
                                }

                                is Adm1ProcessingEvent.Completed -> {
                                    val adm1StartTime = System.currentTimeMillis()
                                    val index = event.index
                                    event.result
                                        .onSuccess { adm1GeoArea ->
                                            // Set parent to country
                                            val areaWithParent = adm1GeoArea.copy(parentId = countryId)

                                            runCatching {
                                                // Upsert ADM1
                                                val adm1Id = geoAreaRepository.upsertArea(areaWithParent)

                                                // Upsert ADM2 children with ADM1 as parent
                                                var adm2Processed = 0
                                                if (adm1GeoArea.children.isNotEmpty()) {
                                                    val adm2WithParent = adm1GeoArea.children.map { it.copy(parentId = adm1Id) }

                                                    // Process ADM2 and send progress for each item (real-time updates)
                                                    adm2WithParent.forEachIndexed { adm2Index, adm2 ->
                                                        runCatching { geoAreaRepository.upsertArea(adm2) }
                                                            .onSuccess { adm2Processed++ }

                                                        // Send ADM2 progress for each item
                                                        val progressEvent = GeoJsonProgressEvent.Adm2Progress(
                                                            adm1Index = index + 1,
                                                            processedCount = adm2Index + 1,
                                                            totalCount = adm2WithParent.size,
                                                            currentAdm2Name = adm2.name,
                                                        )
                                                        send(ServerSentEvent(data = formatter.encodeToString(progressEvent), event = "progress"))
                                                    }
                                                }
                                                adm2Processed to adm1GeoArea.children.size
                                            }
                                                .onSuccess { (processedAdm2, totalAdm2) ->
                                                    successCount++
                                                    totalAdm2Count += totalAdm2
                                                    val completedEvent = GeoJsonProgressEvent.RegionCompleted(
                                                        index = index + 1,
                                                        regionName = adm1GeoArea.name,
                                                        success = true,
                                                        level = 1,
                                                        nameEn = adm1GeoArea.nameEn,
                                                        nameJa = adm1GeoArea.nameJa,
                                                        isoCode = adm1GeoArea.isoCode,
                                                        thumbnailUrl = adm1GeoArea.thumbnailUrl,
                                                        wikipedia = adm1GeoArea.wikipedia,
                                                        centerLat = adm1GeoArea.center?.lat,
                                                        centerLon = adm1GeoArea.center?.lon,
                                                        adm2ProcessedCount = processedAdm2,
                                                        adm2TotalCount = totalAdm2,
                                                        processingTimeMs = System.currentTimeMillis() - adm1StartTime,
                                                    )
                                                    send(ServerSentEvent(data = formatter.encodeToString(completedEvent), event = "progress"))
                                                }
                                                .onFailure { e ->
                                                    failCount++
                                                    val failedEvent = GeoJsonProgressEvent.RegionCompleted(
                                                        index = index + 1,
                                                        regionName = adm1GeoArea.name,
                                                        success = false,
                                                        errorMessage = e.message,
                                                        level = 1,
                                                        nameEn = adm1GeoArea.nameEn,
                                                        nameJa = adm1GeoArea.nameJa,
                                                        isoCode = adm1GeoArea.isoCode,
                                                        thumbnailUrl = adm1GeoArea.thumbnailUrl,
                                                        processingTimeMs = System.currentTimeMillis() - adm1StartTime,
                                                    )
                                                    send(ServerSentEvent(data = formatter.encodeToString(failedEvent), event = "progress"))
                                                }
                                        }
                                        .onFailure { e ->
                                            failCount++
                                            val regionName = regions.getOrNull(index)?.name ?: "Unknown"
                                            val failedEvent = GeoJsonProgressEvent.RegionCompleted(
                                                index = index + 1,
                                                regionName = regionName,
                                                success = false,
                                                errorMessage = e.message,
                                                level = 1,
                                            )
                                            send(ServerSentEvent(data = formatter.encodeToString(failedEvent), event = "progress"))
                                        }
                                }
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
                            level = 0,
                            processingTimeMs = System.currentTimeMillis() - adm0StartTime,
                        )
                        send(ServerSentEvent(data = formatter.encodeToString(event), event = "progress"))
                    }

                val completedEvent = GeoJsonProgressEvent.Completed(
                    successCount = successCount,
                    failCount = failCount,
                    totalProcessingTimeMs = System.currentTimeMillis() - overallStartTime,
                    adm2TotalCount = totalAdm2Count,
                )
                send(ServerSentEvent(data = formatter.encodeToString(completedEvent), event = "progress"))
            } catch (e: Exception) {
                val errorEvent = GeoJsonProgressEvent.Error(e.message ?: "Unknown error occurred")
                send(ServerSentEvent(data = formatter.encodeToString(errorEvent), event = "progress"))
            }
        }
    }
}

/**
 * Build RegionInfo list for initial card display
 */
private fun buildRegionInfoList(
    supportedRegion: SupportedRegion?,
    countryArea: GeoArea,
    regions: List<GeoBoundaryMapper.Adm1Region>,
): List<GeoJsonProgressEvent.RegionInfo> {
    val list = mutableListOf<GeoJsonProgressEvent.RegionInfo>()

    // ADM0 (country)
    list.add(
        GeoJsonProgressEvent.RegionInfo(
            index = 0,
            name = countryArea.name,
            level = 0,
            thumbnailUrl = supportedRegion?.flagUrl,
            adm2Count = 0,
        ),
    )

    // ADM1 regions
    regions.forEachIndexed { index, region ->
        list.add(
            GeoJsonProgressEvent.RegionInfo(
                index = index + 1,
                name = region.name,
                level = 1,
                thumbnailUrl = null, // Will be updated when region is completed
                adm2Count = region.children.size,
            ),
        )
    }

    return list
}
