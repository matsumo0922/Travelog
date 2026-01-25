package me.matsumo.travelog.core.usecase

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.db.CropData
import me.matsumo.travelog.core.model.geo.GeoArea
import platform.CoreGraphics.CGAffineTransformConcat
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGContextAddPath
import platform.CoreGraphics.CGContextClip
import platform.CoreGraphics.CGContextConcatCTM
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGMutablePathRef
import platform.CoreGraphics.CGPathAddLineToPoint
import platform.CoreGraphics.CGPathCloseSubpath
import platform.CoreGraphics.CGPathCreateMutable
import platform.CoreGraphics.CGPathMoveToPoint
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun generateCroppedImage(
    imageBytes: ByteArray,
    geoArea: GeoArea,
    cropData: CropData,
    outputSize: Int,
): ByteArray = withContext(Dispatchers.IO) {
    // Decode source image
    val nsData = imageBytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = imageBytes.size.toULong())
    }
    val sourceImage = UIImage.imageWithData(nsData)
        ?: throw IllegalArgumentException("Failed to decode image")

    // Get areas (either children or self)
    val areas = geoArea.children.takeIf { it.isNotEmpty() } ?: listOf(geoArea)

    // Calculate bounds
    val bounds = calculateBounds(areas) ?: throw IllegalArgumentException("No coordinates found")

    // Calculate viewport transform (no padding for pre-cropped images)
    val transform = calculateViewportTransform(bounds, outputSize.toFloat(), outputSize.toFloat(), padding = 0f)

    // Create bitmap context
    val colorSpace = CGColorSpaceCreateDeviceRGB()
    val context = CGBitmapContextCreate(
        data = null,
        width = outputSize.toULong(),
        height = outputSize.toULong(),
        bitsPerComponent = 8u,
        bytesPerRow = (outputSize * 4).toULong(),
        space = colorSpace,
        bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
    ) ?: throw IllegalStateException("Failed to create bitmap context")

    // Create polygon path
    val clipPath = createPolygonPath(areas, bounds, transform, outputSize)

    CGContextSaveGState(context)

    // Apply clip path
    CGContextAddPath(context, clipPath)
    CGContextClip(context)

    // Calculate draw rect with crop transform
    val (imageWidth, imageHeight) = sourceImage.size.useContents {
        width to height
    }
    val imageAspect = imageWidth / imageHeight

    val drawWidth: Double
    val drawHeight: Double
    if (imageAspect > 1.0) {
        drawHeight = outputSize.toDouble()
        drawWidth = drawHeight * imageAspect
    } else {
        drawWidth = outputSize.toDouble()
        drawHeight = drawWidth / imageAspect
    }

    val centerX = outputSize / 2.0
    val centerY = outputSize / 2.0
    val offsetX = cropData.offsetX * outputSize
    val offsetY = cropData.offsetY * outputSize
    val scale = cropData.scale.toDouble()

    // Apply transforms (note: CGContext uses bottom-left origin)
    val translateToCenter = CGAffineTransformMakeTranslation(centerX + offsetX, centerY - offsetY)
    val scaleTransform = CGAffineTransformMakeScale(scale, scale)
    val translateBack = CGAffineTransformMakeTranslation(-drawWidth / 2.0, -drawHeight / 2.0)

    var combinedTransform = translateToCenter
    combinedTransform = CGAffineTransformConcat(scaleTransform, combinedTransform)
    combinedTransform = CGAffineTransformConcat(translateBack, combinedTransform)

    CGContextConcatCTM(context, combinedTransform)

    // Draw image
    val sourceRef = sourceImage.CGImage
    if (sourceRef != null) {
        val drawRect = CGRectMake(0.0, 0.0, drawWidth, drawHeight)
        CGContextDrawImage(context, drawRect, sourceRef)
    }

    CGContextRestoreGState(context)

    // Create output image
    val outputCGImage = CGBitmapContextCreateImage(context)
        ?: throw IllegalStateException("Failed to create output image")

    val outputImage = UIImage.imageWithCGImage(outputCGImage)
    val jpegData = UIImageJPEGRepresentation(outputImage, 0.9)
        ?: throw IllegalStateException("Failed to encode JPEG")

    // Convert NSData to ByteArray
    val length = jpegData.length.toInt()
    val result = ByteArray(length)
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), jpegData.bytes, length.toULong())
    }

    result
}

private data class Bounds(
    val minLon: Double,
    val maxLon: Double,
    val minLat: Double,
    val maxLat: Double,
)

private data class ViewportTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
)

private fun calculateBounds(areas: List<GeoArea>): Bounds? {
    var minLon = Double.MAX_VALUE
    var maxLon = -Double.MAX_VALUE
    var minLat = Double.MAX_VALUE
    var maxLat = -Double.MAX_VALUE
    var hasCoordinates = false

    areas.forEach { area ->
        area.polygons.forEach { polygon ->
            polygon.forEach { ring ->
                ring.forEach { coordinate ->
                    hasCoordinates = true
                    minLon = min(minLon, coordinate.lon)
                    maxLon = max(maxLon, coordinate.lon)
                    minLat = min(minLat, coordinate.lat)
                    maxLat = max(maxLat, coordinate.lat)
                }
            }
        }
    }

    return if (hasCoordinates) {
        Bounds(minLon, maxLon, minLat, maxLat)
    } else {
        null
    }
}

private fun latToMercator(lat: Double): Double {
    val latRad = lat * PI / 180.0
    return ln(tan(PI / 4.0 + latRad / 2.0)) * 180.0 / PI
}

private fun calculateViewportTransform(
    bounds: Bounds,
    canvasWidth: Float,
    canvasHeight: Float,
    padding: Float = 0.05f,
): ViewportTransform {
    val paddedWidth = canvasWidth * (1f - padding * 2)
    val paddedHeight = canvasHeight * (1f - padding * 2)

    val minLatMerc = latToMercator(bounds.minLat)
    val maxLatMerc = latToMercator(bounds.maxLat)
    val latRangeMerc = maxLatMerc - minLatMerc
    val lonRange = bounds.maxLon - bounds.minLon

    val scaleX = paddedWidth / lonRange.toFloat()
    val scaleY = paddedHeight / latRangeMerc.toFloat()
    val scale = minOf(scaleX, scaleY)

    val contentWidth = (lonRange * scale).toFloat()
    val contentHeight = (latRangeMerc * scale).toFloat()

    val offsetX = (canvasWidth - contentWidth) / 2f
    val offsetY = (canvasHeight - contentHeight) / 2f

    return ViewportTransform(scale, offsetX, offsetY)
}

@OptIn(ExperimentalForeignApi::class)
private fun createPolygonPath(
    areas: List<GeoArea>,
    bounds: Bounds,
    transform: ViewportTransform,
    outputSize: Int,
): CGMutablePathRef {
    val path = CGPathCreateMutable()!!
    val maxLatMerc = latToMercator(bounds.maxLat)

    areas.forEach { area ->
        area.polygons.forEach { polygon ->
            polygon.forEach { ring ->
                if (ring.isEmpty()) return@forEach

                val first = ring.first()
                val startX = transform.offsetX + ((first.lon - bounds.minLon) * transform.scale).toFloat()
                // Flip Y coordinate for CGContext (bottom-left origin)
                val startY = outputSize - (transform.offsetY + ((maxLatMerc - latToMercator(first.lat)) * transform.scale).toFloat())

                CGPathMoveToPoint(path, null, startX.toDouble(), startY.toDouble())

                ring.drop(1).forEach { coord ->
                    val x = transform.offsetX + ((coord.lon - bounds.minLon) * transform.scale).toFloat()
                    val y = outputSize - (transform.offsetY + ((maxLatMerc - latToMercator(coord.lat)) * transform.scale).toFloat())
                    CGPathAddLineToPoint(path, null, x.toDouble(), y.toDouble())
                }
                CGPathCloseSubpath(path)
            }
        }
    }

    return path
}
