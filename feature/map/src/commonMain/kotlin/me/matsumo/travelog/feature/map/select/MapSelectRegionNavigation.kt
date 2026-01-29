package me.matsumo.travelog.feature.map.select

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import me.matsumo.travelog.core.ui.screen.Destination

fun EntryProviderScope<NavKey>.mapSelectRegionEntry() {
    entry<Destination.MapSelectRegion> {
        MapSelectRegionRoute(
            modifier = Modifier.fillMaxSize(),
            mapId = it.mapId,
            geoAreaId = it.geoAreaId,
            initialRegions = it.regions?.toImmutableList(),
            initialRegionImageUrls = it.regionImageUrls?.toImmutableMap(),
        )
    }
}
