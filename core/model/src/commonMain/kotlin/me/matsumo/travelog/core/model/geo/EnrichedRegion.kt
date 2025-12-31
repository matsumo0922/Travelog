package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.Serializable
import me.matsumo.travelog.core.model.geo.OverpassResult.Element.Coordinate

@Serializable
data class EnrichedRegion(
    val id: Long,
    val tags: Map<String, String>,
    val center: Coordinate,
    val polygons: List<PolygonWithHoles>,
    val thumbnailUrl: String?,
)