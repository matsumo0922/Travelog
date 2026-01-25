package me.matsumo.travelog.core.model.db

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Crop transform data for image within a region polygon.
 *
 * Used to store the position and scale of an image relative to the region shape.
 */
@Immutable
@Serializable
data class CropData(
    @SerialName("scale")
    val scale: Float = 1f,

    @SerialName("offset_x")
    val offsetX: Float = 0f,

    @SerialName("offset_y")
    val offsetY: Float = 0f,
)
