package me.matsumo.travelog.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GeoJsonData(
    val type: String,
    val features: List<GeoJsonFeature>,
)

@Serializable
data class GeoJsonFeature(
    val type: String,
    val geometry: GeoJsonGeometry,
    val properties: JsonElement? = null,
)

@Serializable
data class GeoJsonGeometry(
    val type: String,
    val coordinates: JsonElement,
)
