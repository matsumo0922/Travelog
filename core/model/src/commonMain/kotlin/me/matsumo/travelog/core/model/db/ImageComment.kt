package me.matsumo.travelog.core.model.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ImageComment(
    @SerialName("id")
    val id: String? = null,

    @SerialName("image_id")
    val imageId: String,

    @SerialName("author_user_id")
    val authorUserId: String,

    @SerialName("body")
    val body: String,

    @SerialName("created_at")
    val createdAt: Instant? = null,

    @SerialName("updated_at")
    val updatedAt: Instant? = null,

    @SerialName("deleted_at")
    val deletedAt: Instant? = null,
)
