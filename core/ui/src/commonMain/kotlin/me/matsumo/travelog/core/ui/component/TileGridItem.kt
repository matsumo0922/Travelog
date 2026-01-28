package me.matsumo.travelog.core.ui.component

import androidx.compose.runtime.Immutable

interface TileGridItem {
    val id: String
    val spanWidth: Int
    val spanHeight: Int
}

@Immutable
data class TileSpanSize(
    val spanWidth: Int,
    val spanHeight: Int,
    val weight: Float = 1f,
)

@Immutable
data class TileGridConfig(
    val columnCount: Int = 3,
    val specialSizeInterval: Int = 5,
    val availableSpecialSizes: List<TileSpanSize> = listOf(
        TileSpanSize(1, 2),
        TileSpanSize(2, 1),
        TileSpanSize(2, 2, weight = 2f),
        TileSpanSize(2, 3),
        TileSpanSize(3, 2),
    ),
)
