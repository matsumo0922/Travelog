package me.matsumo.travelog.core.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeoRegionGroupDTO(
    @SerialName("id")
    val id: String? = null,
    @SerialName("adm_id")
    val admId: String,
    @SerialName("adm_name")
    val admName: String,
    @SerialName("adm_group")
    val admGroup: String,
    @SerialName("adm_iso")
    val admISO: String,
    @SerialName("name")
    val name: String,
    @SerialName("name_en")
    val nameEn: String? = null,
    @SerialName("name_ja")
    val nameJa: String? = null,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("polygons_geojson")
    val polygonsGeoJson: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)
