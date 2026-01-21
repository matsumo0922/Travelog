package ui.common

import kotlinx.html.BODY
import kotlinx.html.DIV
import kotlinx.html.HEAD
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.h3
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.onClick
import kotlinx.html.script
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.unsafe
import me.matsumo.travelog.core.model.SupportedRegion

/**
 * HEAD 拡張: Tailwind CDN を追加
 */
fun HEAD.tailwindCdn() {
    script(src = "https://cdn.tailwindcss.com") {}
}

/**
 * HEAD 拡張: カスタムスタイルを追加
 */
fun HEAD.customStyles(vararg styles: String) {
    style {
        unsafe {
            raw(styles.joinToString("\n\n"))
        }
    }
}

/**
 * 統計カード
 */
fun DIV.statisticsCard(cardId: String, label: String, value: String, colorClass: String) {
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

/**
 * ログコンテナ
 */
fun DIV.logContainer() {
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
 * 全国カード（ヘッダー）
 */
fun DIV.allCountriesCard(href: String, gradientClasses: String = "from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700") {
    val totalRegions = SupportedRegion.all.sumOf { it.subRegionCount }
    a(
        href = href,
        classes = "bg-gradient-to-r $gradientClasses " +
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

/**
 * 国選択グリッド
 */
fun BODY.countrySelectionGrid(
    hrefBuilder: (SupportedRegion) -> String,
    allHref: String?,
    allGradientClasses: String = "from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700"
) {
    div(classes = "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4") {
        if (allHref != null) {
            allCountriesCard(allHref, allGradientClasses)
        }
        SupportedRegion.all.forEach { region ->
            countryCard(region, hrefBuilder(region))
        }
    }
}

/**
 * 国カード
 */
fun DIV.countryCard(region: SupportedRegion, href: String) {
    a(
        href = href,
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

/**
 * 国カード（インデックス付き、進捗表示用）
 */
fun DIV.countryCardWithProgress(index: Int, region: SupportedRegion, progressBarColor: String = "bg-blue-400") {
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
                div(classes = "$progressBarColor h-full progress-fill") {
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

/**
 * プログレスコンテナ
 */
fun DIV.progressContainer(
    gradientClasses: String = "from-blue-500 to-green-500",
    statusId: String = "status",
    fillId: String = "progressFill",
    initialStatus: String = "Ready to start"
) {
    div(classes = "bg-white rounded-lg p-5 mb-5 shadow-sm") {
        div(classes = "bg-gray-200 rounded-full h-4 overflow-hidden") {
            div(classes = "bg-gradient-to-r $gradientClasses h-full progress-fill") {
                id = fillId
                style = "width: 0%"
            }
        }
        div(classes = "mt-3 text-gray-600 text-sm") {
            id = statusId
            +initialStatus
        }
    }
}
