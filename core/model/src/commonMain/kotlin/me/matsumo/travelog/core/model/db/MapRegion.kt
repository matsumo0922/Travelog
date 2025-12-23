package me.matsumo.travelog.core.model.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class MapRegion(
    @SerialName("id")
    val id: String? = null,

    @SerialName("map_id")
    val mapId: String,

    @SerialName("boundary_external_id")
    val boundaryExternalId: String,

    @SerialName("representative_image_id")
    val representativeImageId: String?,

    @SerialName("created_at")
    val createdAt: Instant? = null,

    @SerialName("updated_at")
    val updatedAt: Instant? = null,
)
