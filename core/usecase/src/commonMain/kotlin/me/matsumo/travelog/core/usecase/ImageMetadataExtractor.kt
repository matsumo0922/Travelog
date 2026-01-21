package me.matsumo.travelog.core.usecase

import io.github.vinceglb.filekit.PlatformFile

data class ImageMetadata(
    val width: Int,
    val height: Int,
)

expect suspend fun extractImageMetadata(file: PlatformFile): ImageMetadata?
