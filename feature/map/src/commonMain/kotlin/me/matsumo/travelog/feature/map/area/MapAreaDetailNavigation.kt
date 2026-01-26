package me.matsumo.travelog.feature.map.area

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.matsumo.travelog.core.ui.screen.Destination

fun EntryProviderScope<NavKey>.mapAreaDetailEntry() {
    entry<Destination.MapAreaDetail> {
        MapAreaDetailRoute(
            modifier = Modifier.fillMaxSize(),
            mapId = it.mapId,
            geoAreaId = it.geoAreaId,
            initialRegions = it.regions,
            initialRegionImageUrls = it.regionImageUrls,
        )
    }
}
