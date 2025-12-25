package me.matsumo.travelog.core.model

import androidx.compose.runtime.Stable
import me.matsumo.travelog.core.resource.Res
import org.jetbrains.compose.resources.StringResource

@Stable
data class SupportedCountry(
    val code: String,
    val nameRes: StringResource,
) {
    companion object {
        val all = listOf(
            SupportedCountry("JP", Res.string.country_japan),
        )
    }
}