package me.matsumo.travelog.core.model.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Map(
    @SerialName("id")
    val id: String? = null,

    @SerialName("owner_user_id")
    val ownerUserId: String,

    @SerialName("root_boundary_external_id")
    val rootBoundaryExternalId: String,

    @SerialName("country_code")
    val countryCode: String?,

    @SerialName("title")
    val title: String,

    @SerialName("description")
    val description: String?,

    @SerialName("icon_image_id")
    val iconImageId: String?,

    @SerialName("created_at")
    val createdAt: Instant? = null,

    @SerialName("updated_at")
    val updatedAt: Instant? = null,
)
