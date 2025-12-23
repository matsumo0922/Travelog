package me.matsumo.travelog.core.model.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.time.Instant

@Serializable
data class Image(
    @SerialName("id")
    val id: String? = null,

    @SerialName("uploader_user_id")
    val uploaderUserId: String,

    @SerialName("map_region_id")
    val mapRegionId: String?,

    @SerialName("storage_key")
    val storageKey: String,

    @SerialName("content_type")
    val contentType: String?,

    @SerialName("file_size")
    val fileSize: Long?,

    @SerialName("width")
    val width: Int?,

    @SerialName("height")
    val height: Int?,

    @SerialName("taken_at")
    val takenAt: Instant?,

    @SerialName("taken_lat")
    val takenLat: Double?,

    @SerialName("taken_lng")
    val takenLng: Double?,

    @SerialName("exif")
    val exif: JsonElement?,

    @SerialName("created_at")
    val createdAt: Instant? = null,
)
