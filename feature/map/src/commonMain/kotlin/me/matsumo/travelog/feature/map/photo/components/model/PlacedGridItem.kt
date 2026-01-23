package me.matsumo.travelog.feature.map.photo.components.model

import androidx.compose.runtime.Immutable

@Immutable
data class PlacedGridItem(
    val item: GridPhotoItem,
    val column: Int,
    val row: Int,
    val spanWidth: Int,
    val spanHeight: Int,
)
