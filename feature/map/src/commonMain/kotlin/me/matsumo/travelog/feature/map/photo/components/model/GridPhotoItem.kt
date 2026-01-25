package me.matsumo.travelog.feature.map.photo.components.model

import androidx.compose.runtime.Immutable
import me.matsumo.travelog.core.ui.component.TileGridItem

@Immutable
data class GridPhotoItem(
    override val id: String,
    val imageUrl: String,
    override val spanWidth: Int = 1,
    override val spanHeight: Int = 1,
) : TileGridItem
