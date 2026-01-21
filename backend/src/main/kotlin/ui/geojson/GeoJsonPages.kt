package ui.geojson

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
import ui.common.PageStyles
import ui.common.countryCardWithProgress
import ui.common.logContainer
import ui.common.statisticsCard
import ui.common.tailwindCdn

/**
 * GeoJSON リージョン一覧ページ
 */
fun HTML.regionListPage() {
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

/**
 * GeoJSON 進捗ページ（単一国）
 */
fun HTML.progressPage(country: String) {
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

/**
 * GeoJSON 全国処理進捗ページ
 */
fun HTML.allCountriesProgressPage(mode: String) {
    val totalRegions = SupportedRegion.all.sumOf { it.subRegionCount }
    head {
        meta(charset = "UTF-8")
        title("$mode Processing - All Countries")
        tailwindCdn()
        allCountriesPageStyles()
    }
    body(classes = "min-h-screen bg-gray-100") {
        div(classes = "max-w-6xl mx-auto py-10 px-5") {
            h1(classes = "text-gray-800 text-2xl font-bold mb-4") {
                +"${mode.replaceFirstChar { it.uppercase() }} Processing: All Countries"
            }
            p(classes = "text-gray-600 mb-6") {
                +"Processing ${SupportedRegion.all.size} countries with $totalRegions regions total"
            }

            // Control buttons
            allCountriesControlButtons(mode)

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
                    div(classes = "bg-gradient-to-r from-blue-500 to-purple-600 h-full progress-fill") {
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
                        countryCardWithProgress(index, region, "bg-blue-400")
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
                        div(classes = "bg-gradient-to-r from-green-400 to-blue-500 h-full progress-fill") {
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

// ============= Private Helper Functions =============

private fun HEAD.progressPageStyles() {
    style {
        unsafe {
            raw(
                listOf(
                    PageStyles.logStyles,
                    PageStyles.regionCardStateStyles,
                    PageStyles.animationStyles,
                    PageStyles.enrichmentStatusStyles,
                ).joinToString("\n\n"),
            )
        }
    }
}

private fun HEAD.allCountriesPageStyles() {
    style {
        unsafe {
            raw(
                listOf(
                    PageStyles.logStyles,
                    PageStyles.countryCardStateStyles,
                    PageStyles.animationStyles,
                ).joinToString("\n\n"),
            )
        }
    }
}

private fun BODY.regionList() {
    div(classes = "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4") {
        allCountriesCardForGeoJson("/geojson/all")
        SupportedRegion.all.forEach { region ->
            regionCard(region)
        }
    }
}

private fun DIV.allCountriesCardForGeoJson(href: String) {
    val totalRegions = SupportedRegion.all.sumOf { it.subRegionCount }
    a(
        href = href,
        classes = "bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 " +
                "rounded-lg p-4 no-underline text-white transition-all flex items-center gap-3 shadow-lg",
    ) {
        div(classes = "w-12 h-9 flex items-center justify-center text-2xl") {
            +"🌍"
        }
        div(classes = "flex-1") {
            div(classes = "font-semibold mb-1") { +"All Countries" }
            div(classes = "text-xs text-blue-100") { +"${SupportedRegion.all.size} countries, $totalRegions regions" }
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

private fun DIV.statisticsBar() {
    div(classes = "grid grid-cols-4 gap-4 mb-5") {
        statisticsCard("totalStat", "Total Regions", "0", "bg-blue-500")
        statisticsCard("successStat", "Success", "0", "bg-green-500")
        statisticsCard("failedStat", "Failed", "0", "bg-red-500")
        statisticsCard("timeStat", "Elapsed Time", "00:00", "bg-purple-500")
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

private fun DIV.allCountriesControlButtons(mode: String) {
    div(classes = "flex gap-3 mb-5 items-center flex-wrap") {
        button(
            classes = "bg-green-500 hover:bg-green-600 text-white font-semibold py-2 px-6 rounded-lg " +
                    "transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed",
        ) {
            id = "startBtn"
            onClick = "startAllCountriesProcessing('$mode')"
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

        if (mode == "geojson") {
            // Auto-enrich checkbox for geojson mode
            label(classes = "flex items-center gap-2 text-gray-600 ml-4 cursor-pointer") {
                input(type = InputType.checkBox, classes = "w-4 h-4 rounded border-gray-300") {
                    id = "autoEnrichCheckbox"
                }
                +"完了後に名前補完も全国一括実行"
            }
        }

        // Batch size selector for geo-names mode
        if (mode == "geo-names") {
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
}
