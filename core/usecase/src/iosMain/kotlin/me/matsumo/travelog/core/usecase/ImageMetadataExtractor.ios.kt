package me.matsumo.travelog.core.usecase

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
actual suspend fun extractImageMetadata(file: PlatformFile): ImageMetadata? {
    return runCatching {
        val bytes = file.readBytes()
        val nsData = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }

        val image = UIImage.imageWithData(nsData) ?: return@runCatching null

        val (width, height) = image.size.useContents {
            width.toInt() to height.toInt()
        }

        if (width > 0 && height > 0) {
            ImageMetadata(
                width = width,
                height = height,
            )
        } else {
            null
        }
    }.getOrNull()
}
