package me.matsumo.travelog.core.model.db

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * MapRegion entity representing a selected region within a map.
 *
 * References geo_areas via geo_area_id for geographical data.
 */
@Immutable
@Serializable
data class MapRegion(
    @SerialName("id")
    val id: String? = null,

    @SerialName("map_id")
    val mapId: String,

    /**
     * UUID reference to geo_areas table.
     * Replaced TEXT-based boundary_external_id for proper FK relationship.
     */
    @SerialName("geo_area_id")
    val geoAreaId: String,

    @SerialName("representative_image_id")
    val representativeImageId: String? = null,

    @SerialName("created_at")
    val createdAt: Instant? = null,

    @SerialName("updated_at")
    val updatedAt: Instant? = null,

    @SerialName("crop_data")
    val cropData: CropData? = null,
)
