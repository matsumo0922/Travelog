package me.matsumo.travelog.feature.setting

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.matsumo.travelog.core.model.Destination
import me.matsumo.travelog.core.ui.screen.Destination2

fun NavGraphBuilder.settingScreen() {
    composable<Destination.Setting.Root> {
        SettingScreen(
            modifier = Modifier.fillMaxSize(),
        )
    }
}

fun EntryProviderScope<NavKey>.settingEntry() {
    entry<Destination2.Setting.Root> {
        SettingScreen(
            modifier = Modifier.fillMaxSize(),
        )
    }
}
