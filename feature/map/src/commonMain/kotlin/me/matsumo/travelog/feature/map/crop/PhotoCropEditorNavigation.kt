package me.matsumo.travelog.feature.map.crop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.matsumo.travelog.core.ui.screen.Destination

fun EntryProviderScope<NavKey>.photoCropEditorEntry() {
    entry<Destination.PhotoCropEditor> {
        PhotoCropEditorRoute(
            modifier = Modifier.fillMaxSize(),
            mapId = it.mapId,
            geoAreaId = it.geoAreaId,
            localFilePath = it.localFilePath,
            existingRegionId = it.existingRegionId,
        )
    }
}
