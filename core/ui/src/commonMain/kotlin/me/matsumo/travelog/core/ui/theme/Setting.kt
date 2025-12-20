package me.matsumo.travelog.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import me.matsumo.travelog.core.model.AppConfig
import me.matsumo.travelog.core.model.AppSetting

val LocalAppSetting = staticCompositionLocalOf {
    AppSetting.DEFAULT
}

val LocalAppConfig = staticCompositionLocalOf<AppConfig> {
    error("No AppConfig provided")
}
