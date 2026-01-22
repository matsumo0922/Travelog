package me.matsumo.travelog.core.model.db

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Instant

/**
 * Map entity representing a user-created map collection.
 *
 * References geo_areas via root_geo_area_id for geographical scope.
 * Country code can be derived from the referenced geo_area.
 */
@Immutable
@Serializable
data class Map(
    @SerialName("id")
    val id: String? = null,

    @SerialName("owner_user_id")
    val ownerUserId: String,

    /**
     * UUID reference to geo_areas table.
     * Replaced TEXT-based root_boundary_external_id for proper FK relationship.
     */
    @SerialName("root_geo_area_id")
    val rootGeoAreaId: String,

    @SerialName("title")
    val title: String,

    @SerialName("description")
    val description: String? = null,

    @SerialName("icon_image_id")
    val iconImageId: String? = null,

    @Transient
    val iconImageUrl: String? = null,

    @SerialName("created_at")
    val createdAt: Instant? = null,

    @SerialName("updated_at")
    val updatedAt: Instant? = null,
)
