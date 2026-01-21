package me.matsumo.travelog.core.usecase

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.serialization.json.JsonObject
import kotlin.time.Instant

data class ImageMetadata(
    val width: Int,
    val height: Int,
    val takenAt: Instant? = null,
    val takenLat: Double? = null,
    val takenLng: Double? = null,
    val exif: JsonObject? = null,
)

expect suspend fun extractImageMetadata(file: PlatformFile): ImageMetadata?
