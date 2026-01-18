package me.matsumo.travelog.feature.home.select.region

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.matsumo.travelog.core.ui.screen.Destination

fun EntryProviderScope<NavKey>.regionSelectEntry() {
    entry<Destination.RegionSelect> {
        RegionSelectRoute(
            modifier = Modifier.fillMaxSize(),
            selectedCountryCode3 = it.selectedCountryCode3
        )
    }
}