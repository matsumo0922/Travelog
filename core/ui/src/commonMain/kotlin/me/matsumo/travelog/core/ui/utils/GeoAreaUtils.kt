package me.matsumo.travelog.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import me.matsumo.travelog.core.model.geo.GeoArea

/**
 * Get display name with fallback order: name_ja -> name_en -> name
 */
@Composable
fun GeoArea.getLocalizedName(): String {
    return if (Locale.current.language == "ja") {
        nameJa ?: name
    } else {
        nameEn ?: name
    }
}
