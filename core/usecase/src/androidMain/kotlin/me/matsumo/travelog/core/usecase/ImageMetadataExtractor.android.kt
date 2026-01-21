package me.matsumo.travelog.core.usecase

import android.graphics.BitmapFactory
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes

actual suspend fun extractImageMetadata(file: PlatformFile): ImageMetadata? {
    return runCatching {
        val bytes = file.readBytes()
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        if (options.outWidth > 0 && options.outHeight > 0) {
            ImageMetadata(
                width = options.outWidth,
                height = options.outHeight,
            )
        } else {
            null
        }
    }.getOrNull()
}
