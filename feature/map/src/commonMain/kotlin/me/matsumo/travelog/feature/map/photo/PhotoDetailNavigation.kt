package me.matsumo.travelog.feature.map.photo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.matsumo.travelog.core.ui.screen.Destination

fun EntryProviderScope<NavKey>.photoDetailEntry() {
    entry<Destination.PhotoDetail> {
        PhotoDetailRoute(
            modifier = Modifier.fillMaxSize(),
            imageId = it.imageId,
            imageUrl = it.imageUrl,
            regionName = it.regionName,
        )
    }
}
