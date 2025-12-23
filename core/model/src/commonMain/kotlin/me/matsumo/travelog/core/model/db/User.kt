package me.matsumo.travelog.core.model.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class User(
    @SerialName("id")
    val id: String? = null, // auth.uid()

    @SerialName("handle")
    val handle: String,

    @SerialName("display_name")
    val displayName: String,

    @SerialName("icon_image_id")
    val iconImageId: String?,

    @SerialName("created_at")
    val createdAt: Instant? = null,

    @SerialName("updated_at")
    val updatedAt: Instant? = null,
)
