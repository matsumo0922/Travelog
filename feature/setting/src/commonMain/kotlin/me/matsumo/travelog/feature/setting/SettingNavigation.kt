package me.matsumo.travelog.feature.setting

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.matsumo.travelog.core.ui.screen.Destination

fun EntryProviderScope<NavKey>.settingEntry() {
    entry<Destination.Setting.Root> {
        SettingScreen(
            modifier = Modifier.fillMaxSize(),
        )
    }
}
