package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NominatimResult(
    @SerialName("boundingbox")
    val boundingbox: List<String>,
    @SerialName("category")
    val category: String,
    @SerialName("country_code")
    val countryCode: String? = null,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("lat")
    val lat: String,
    @SerialName("lon")
    val lon: String,
    @SerialName("name")
    val name: String,
    @SerialName("osm_id")
    val osmId: Long,
    @SerialName("place_id")
    val placeId: Long,
    @SerialName("place_rank")
    val placeRank: Int,
    @SerialName("type")
    val type: String,
) {
    val center: OverpassResult.Element.Coordinate
        get() = OverpassResult.Element.Coordinate(
            lat = lat.toDouble(),
            lon = lon.toDouble(),
        )
}
