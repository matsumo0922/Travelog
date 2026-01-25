package me.matsumo.travelog.core.usecase

import me.matsumo.travelog.core.model.db.CropData
import me.matsumo.travelog.core.model.geo.GeoArea

/**
 * Generates a cropped image clipped to a geographic region's polygon shape.
 *
 * @param imageBytes The source image bytes
 * @param geoArea The geographic area whose polygon shape will be used for clipping
 * @param cropData The crop transform data (scale and offset)
 * @param outputSize The output image size in pixels (square)
 * @return JPEG encoded bytes of the cropped image
 */
expect suspend fun generateCroppedImage(
    imageBytes: ByteArray,
    geoArea: GeoArea,
    cropData: CropData,
    outputSize: Int = 512,
): ByteArray
