package me.matsumo.travelog.feature.map.setting

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.matsumo.travelog.core.ui.screen.Destination

fun EntryProviderScope<NavKey>.mapSettingEntry() {
    entry<Destination.MapSetting> {
        MapSettingScreen(
            modifier = Modifier.fillMaxSize(),
            mapId = it.mapId,
            initialMap = it.map,
            initialGeoAreaId = it.geoAreaId,
            initialGeoAreaName = it.geoAreaName,
            initialTotalChildCount = it.totalChildCount,
            initialRegions = it.regions,
        )
    }
}
