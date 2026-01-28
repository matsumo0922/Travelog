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

    /**
     * Editor viewport size used to compute the transform.
     * Needed to reproduce the UI crop when generating the pre-cropped image.
     */
    @SerialName("view_width")
    val viewWidth: Float = 0f,

    @SerialName("view_height")
    val viewHeight: Float = 0f,

    /**
     * Padding used when rendering the geo boundary in the editor.
     */
    @SerialName("viewport_padding")
    val viewportPadding: Float = 0.1f,

    /**
     * Rotation angle in degrees (0-360).
     * Applied around the center of the image.
     */
    @SerialName("rotation")
    val rotation: Float = 0f,
)
