package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.matsumo.travelog.core.model.geo.OverpassResult.Element.Coordinate

@Serializable
data class EnrichedRegion(
    @SerialName("id")
    val id: Long,
    @SerialName("tags")
    val tags: Tag,
    @SerialName("center")
    val center: Coordinate,
    @SerialName("polygons")
    val polygons: List<PolygonWithHoles>,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String?,
) {
    @Serializable
    data class Tag(
        @SerialName("name")
        val name: String? = null,
        @SerialName("adm2_id")
        val adm2Id: String? = null,
        @SerialName("name_en")
        val nameEn: String? = null,
        @SerialName("name_ja")
        val nameJa: String? = null,
        @SerialName("wikipedia")
        val wikipedia: String? = null,
        @SerialName("iso3166_2")
        val iso31662: String? = null,
    )
}