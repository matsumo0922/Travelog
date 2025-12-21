package me.matsumo.travelog.core.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import me.matsumo.travelog.core.common.serializer.ColorSerializer

@Serializable
data class AppSetting(
    val id: String,
    val theme: Theme,
    val useDynamicColor: Boolean,
    @Serializable(with = ColorSerializer::class)
    val seedColor: Color,
    val plusMode: Boolean,
    val developerMode: Boolean,
    val useGeoJsonCache: Boolean,
) {
    val hasPrivilege get() = plusMode || developerMode

    companion object {
        val DEFAULT = AppSetting(
            id = "",
            theme = Theme.System,
            useDynamicColor = currentPlatform == Platform.Android,
            seedColor = Color(0xFF7FD0FF),
            plusMode = false,
            developerMode = false,
            useGeoJsonCache = true,
        )
    }
}
