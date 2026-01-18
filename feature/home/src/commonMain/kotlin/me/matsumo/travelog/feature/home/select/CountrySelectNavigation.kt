package me.matsumo.travelog.feature.home.select

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.matsumo.travelog.core.ui.screen.Destination

fun EntryProviderScope<NavKey>.countrySelectEntry() {
    entry<Destination.CountrySelect> {
        CountrySelectScreen(
            modifier = Modifier.fillMaxSize(),
        )
    }
}