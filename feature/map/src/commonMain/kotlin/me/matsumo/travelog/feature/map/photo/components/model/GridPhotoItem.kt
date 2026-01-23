package me.matsumo.travelog.feature.map.photo.components.model

import androidx.compose.runtime.Immutable

@Immutable
data class GridPhotoItem(
    val id: String,
    val imageUrl: String,
    val spanWidth: Int = 1,
    val spanHeight: Int = 1,
)
