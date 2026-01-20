package me.matsumo.travelog.core.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for geo_areas table / geo_areas_view.
 * Maps directly to database columns.
 */
@Serializable
data class GeoAreaDTO(
    val id: String? = null,

    @SerialName("parent_id")
    val parentId: String? = null,

    val level: Int,

    @SerialName("adm_id")
    val admId: String,

    @SerialName("country_code")
    val countryCode: String,

    val name: String,

    @SerialName("name_en")
    val nameEn: String? = null,

    @SerialName("name_ja")
    val nameJa: String? = null,

    @SerialName("iso_code")
    val isoCode: String? = null,

    val wikipedia: String? = null,

    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,

    /**
     * GeoJSON string of Point, from SQL: ST_AsGeoJSON(center::geometry)
     */
    @SerialName("center_geojson")
    val centerGeoJson: String? = null,

    /**
     * GeoJSON string of MultiPolygon, from SQL: ST_AsGeoJSON(polygons)
     */
    @SerialName("polygons_geojson")
    val polygonsGeoJson: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,
)
