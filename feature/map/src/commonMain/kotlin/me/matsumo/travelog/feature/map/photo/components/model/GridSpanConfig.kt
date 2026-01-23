package me.matsumo.travelog.feature.map.photo.components.model

import androidx.compose.runtime.Immutable

@Immutable
data class SpanSize(
    val spanWidth: Int,
    val spanHeight: Int,
    val weight: Float = 1f,
)

@Immutable
data class GridSpanConfig(
    val columnCount: Int = 4,
    val specialSizeInterval: Int = 10,
    val availableSpecialSizes: List<SpanSize> = listOf(
        SpanSize(2, 2, weight = 2f),
        SpanSize(2, 3, weight = 1f),
        SpanSize(3, 2, weight = 1f),
    ),
)
