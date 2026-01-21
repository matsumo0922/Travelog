package me.matsumo.travelog.core.usecase

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun extractImageMetadata(file: PlatformFile): ImageMetadata? {
    return runCatching {
        val bytes = file.readBytes()
        val nsData = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }

        // Get image dimensions using UIImage
        val image = UIImage.imageWithData(nsData) ?: return@runCatching null
        val (width, height) = image.size.useContents {
            width.toInt() to height.toInt()
        }

        if (width <= 0 || height <= 0) {
            return@runCatching null
        }

        // Note: CGImageSource APIs require explicit bridging in Kotlin/Native
        // For simplicity, we return only dimensions and rely on server-side EXIF extraction
        ImageMetadata(
            width = width,
            height = height,
            takenAt = null,
            takenLat = null,
            takenLng = null,
            exif = null,
        )
    }.getOrNull()
}
