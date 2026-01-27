// iosMain/kotlin/me/matsumo/travelog/core/usecase/GenerateCroppedImage.ios.kt

package me.matsumo.travelog.core.usecase

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.db.CropData
import me.matsumo.travelog.core.model.geo.GeoArea
import platform.CoreGraphics.CGAffineTransform
import platform.CoreGraphics.CGAffineTransformConcat
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.CoreGraphics.CGAffineTransformScale
import platform.CoreGraphics.CGAffineTransformTranslate
import platform.CoreGraphics.CGContextAddPath
import platform.CoreGraphics.CGContextClip
import platform.CoreGraphics.CGContextConcatCTM
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGImageCreateWithImageInRect
import platform.CoreGraphics.CGPathAddLineToPoint
import platform.CoreGraphics.CGPathCloseSubpath
import platform.CoreGraphics.CGPathCreateMutable
import platform.CoreGraphics.CGPathMoveToPoint
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

// Note: iOS implementation assumes platform linkage with UIKit, CoreGraphics, Foundation.

@OptIn(ExperimentalForeignApi::class)
actual suspend fun generateCroppedImage(
    imageBytes: ByteArray,
    geoArea: GeoArea,
    cropData: CropData,
    outputSize: Int,
): ByteArray = withContext(Dispatchers.Default) {
    // 1. Decode Image
    // UIImage(data:) automatically handles EXIF orientation.
    val uiImage = imageBytes.toNSData().let { UIImage.imageWithData(it) } ?: run {
        return@withContext imageBytes
    }

    // Ensure we work with the oriented dimensions
    val bitmapWidth = uiImage.size.useContents { width * uiImage.scale }.toInt()
    val bitmapHeight = uiImage.size.useContents { height * uiImage.scale }.toInt()

    val areas = geoArea.children.takeIf { it.isNotEmpty() } ?: listOf(geoArea)
    val bounds = calculateBounds(areas) ?: run {
        return@withContext imageBytes
    }

    val viewWidth = cropData.viewWidth.takeIf { it > 0f } ?: outputSize.toFloat()
    val viewHeight = cropData.viewHeight.takeIf { it > 0f } ?: outputSize.toFloat()
    val uiPadding = cropData.viewportPadding.takeIf { it > 0f } ?: DEFAULT_UI_PADDING

    val uiTransform = calculateViewportTransform(
        bounds = bounds,
        canvasWidth = viewWidth,
        canvasHeight = viewHeight,
        padding = uiPadding,
    )
    val outputTransform = calculateViewportTransform(
        bounds = bounds,
        canvasWidth = outputSize.toFloat(),
        canvasHeight = outputSize.toFloat(),
        padding = OUTPUT_PADDING,
    )

    val scaleRatio = if (uiTransform.scale > 0f) outputTransform.scale / uiTransform.scale else 1f

    val fitScale = calculateFitScale(viewWidth, viewHeight, bitmapWidth, bitmapHeight)
    val cropScale = calculateCropScale(viewWidth, viewHeight, bitmapWidth, bitmapHeight)
    val zoomScaleFit = if (fitScale > 0f) cropData.scale * (cropScale / fitScale) else cropData.scale
    val offsetXFit = cropData.offsetX * viewWidth
    val offsetYFit = cropData.offsetY * viewHeight

    // 2. Setup Context for Drawing
    // scale=1.0 ensures we work in exact pixels matching outputSize
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(outputSize.toDouble(), outputSize.toDouble()), false, 1.0)
    val context = UIGraphicsGetCurrentContext()

    // 3. Build Matrix (Transform)
    val matrix = buildImageMatrix(
        bitmapWidth = bitmapWidth,
        bitmapHeight = bitmapHeight,
        viewWidth = viewWidth,
        viewHeight = viewHeight,
        fitScale = fitScale,
        zoomScaleFit = zoomScaleFit,
        offsetXFit = offsetXFit,
        offsetYFit = offsetYFit,
        scaleRatio = scaleRatio,
        uiTransform = uiTransform,
        outputTransform = outputTransform,
    )

    // 4. Create Clip Path
    val clipPath = createGeoPath(areas, bounds, outputTransform)

    // 5. Draw
    CGContextSaveGState(context)
    CGContextAddPath(context, clipPath)
    CGContextClip(context)

    // Apply transformation matrix to the context
    // iOS draws UIImage at (0,0) by default, so we transform the CTM to move the image.
    // Note: UIImage drawing handles the internal CGImage orientation automatically.
    // We must flip the context if we were drawing a CGImage directly, but UIGraphics context + UIImage.draw is standard.
    // However, we are using CGContextConcatCTM which affects the coordinate system.
    // Since UIGraphicsBeginImageContext is Top-Left (like Android), the math matches.
    CGContextConcatCTM(context, matrix)

    // Draw the image at 0,0 in the transformed space
    uiImage.drawInRect(CGRectMake(0.0, 0.0, bitmapWidth.toDouble(), bitmapHeight.toDouble()))

    CGContextRestoreGState(context)

    val outputImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    if (outputImage == null) return@withContext imageBytes

    // 6. Crop Logic (Post-processing)
    val maxLatMerc = latToMercator(bounds.maxLat)
    val minLatMerc = latToMercator(bounds.minLat)
    val contentWidth = (bounds.lonRange * outputTransform.scale).toFloat()
    val contentHeight = ((maxLatMerc - minLatMerc) * outputTransform.scale).toFloat()
    val contentLeft = outputTransform.offsetX
    val contentTop = outputTransform.offsetY

    val cropLeft = contentLeft.toInt().coerceIn(0, outputSize - 1)
    val cropTop = contentTop.toInt().coerceIn(0, outputSize - 1)
    val cropRight = (contentLeft + contentWidth).toInt().coerceIn(cropLeft + 1, outputSize)
    val cropBottom = (contentTop + contentHeight).toInt().coerceIn(cropTop + 1, outputSize)
    val cropWidth = cropRight - cropLeft
    val cropHeight = cropBottom - cropTop

    // Crop the result
    val cgImage = outputImage.CGImage
    val cropRect = CGRectMake(
        cropLeft.toDouble(),
        cropTop.toDouble(),
        cropWidth.toDouble(),
        cropHeight.toDouble()
    )

    val croppedCGImage = CGImageCreateWithImageInRect(cgImage, cropRect)
    val croppedUIImage = croppedCGImage?.let { UIImage.imageWithCGImage(it) } ?: outputImage

    // 7. Scale back to outputSize if needed
    val finalImage = if (cropWidth != outputSize || cropHeight != outputSize) {
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(outputSize.toDouble(), outputSize.toDouble()), false, 1.0)
        croppedUIImage.drawInRect(CGRectMake(0.0, 0.0, outputSize.toDouble(), outputSize.toDouble()))
        val scaled = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        scaled ?: croppedUIImage
    } else {
        croppedUIImage
    }

    // 8. Compress to JPEG
    val jpegData = UIImageJPEGRepresentation(finalImage, JPEG_QUALITY / 100.0)
    jpegData?.toByteArray() ?: imageBytes
}

// --- Helpers ---

private const val DEFAULT_UI_PADDING = 0.1f
private const val OUTPUT_PADDING = 0f
private const val JPEG_QUALITY = 92

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned {
    NSData.dataWithBytes(it.addressOf(0), this.size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return bytes
}

private data class Bounds(
    val minLon: Double,
    val maxLon: Double,
    val minLat: Double,
    val maxLat: Double,
) {
    val lonRange: Double get() = maxLon - minLon
}

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
    padding: Float,
): ViewportTransform {
    val paddedWidth = canvasWidth * (1f - padding * 2)
    val paddedHeight = canvasHeight * (1f - padding * 2)

    val minLatMerc = latToMercator(bounds.minLat)
    val maxLatMerc = latToMercator(bounds.maxLat)
    val latRangeMerc = maxLatMerc - minLatMerc

    val scaleX = paddedWidth / bounds.lonRange.toFloat()
    val scaleY = paddedHeight / latRangeMerc.toFloat()
    val scale = min(scaleX, scaleY)

    val contentWidth = (bounds.lonRange * scale).toFloat()
    val contentHeight = (latRangeMerc * scale).toFloat()
    val offsetX = (canvasWidth - contentWidth) / 2f
    val offsetY = (canvasHeight - contentHeight) / 2f

    return ViewportTransform(scale, offsetX, offsetY)
}

@OptIn(ExperimentalForeignApi::class)
private fun createGeoPath(
    areas: List<GeoArea>,
    bounds: Bounds,
    transform: ViewportTransform,
): platform.CoreGraphics.CGPathRef? {
    val path = CGPathCreateMutable()
    val maxLatMerc = latToMercator(bounds.maxLat)

    areas.forEach { area ->
        area.polygons.forEach { polygon ->
            polygon.forEach { ring ->
                if (ring.isEmpty()) return@forEach
                val first = ring.first()
                val startX = transform.offsetX + ((first.lon - bounds.minLon) * transform.scale).toFloat()
                val startY = transform.offsetY + ((maxLatMerc - latToMercator(first.lat)) * transform.scale).toFloat()
                CGPathMoveToPoint(path, null, startX.toDouble(), startY.toDouble())

                ring.drop(1).forEach { coordinate ->
                    val x = transform.offsetX + ((coordinate.lon - bounds.minLon) * transform.scale).toFloat()
                    val y = transform.offsetY + ((maxLatMerc - latToMercator(coordinate.lat)) * transform.scale).toFloat()
                    CGPathAddLineToPoint(path, null, x.toDouble(), y.toDouble())
                }
                CGPathCloseSubpath(path)
            }
        }
    }

    return path
}

private fun calculateFitScale(
    viewWidth: Float,
    viewHeight: Float,
    imageWidth: Int,
    imageHeight: Int,
): Float {
    if (viewWidth <= 0f || viewHeight <= 0f || imageWidth <= 0 || imageHeight <= 0) return 1f
    val scaleX = viewWidth / imageWidth.toFloat()
    val scaleY = viewHeight / imageHeight.toFloat()
    return min(scaleX, scaleY)
}

private fun calculateCropScale(
    viewWidth: Float,
    viewHeight: Float,
    imageWidth: Int,
    imageHeight: Int,
): Float {
    if (viewWidth <= 0f || viewHeight <= 0f || imageWidth <= 0 || imageHeight <= 0) return 1f
    val scaleX = viewWidth / imageWidth.toFloat()
    val scaleY = viewHeight / imageHeight.toFloat()
    return max(scaleX, scaleY)
}

@OptIn(ExperimentalForeignApi::class)
private fun buildImageMatrix(
    bitmapWidth: Int,
    bitmapHeight: Int,
    viewWidth: Float,
    viewHeight: Float,
    fitScale: Float,
    zoomScaleFit: Float,
    offsetXFit: Float,
    offsetYFit: Float,
    scaleRatio: Float,
    uiTransform: ViewportTransform,
    outputTransform: ViewportTransform,
): CGAffineTransform {
    // Android: matrix.post... means Apply After.
    // M' = M * T.
    // iOS CGAffineTransformConcat(t1, t2) -> t1 * t2.
    // So we can chain them in the exact same logical order.

    val fitOffsetX = (viewWidth - bitmapWidth * fitScale) / 2f
    val fitOffsetY = (viewHeight - bitmapHeight * fitScale) / 2f
    val centerX = viewWidth / 2f
    val centerY = viewHeight / 2f

    // 1. matrix.setScale(fitScale, fitScale)
    var transform = CGAffineTransformMakeScale(fitScale.toDouble(), fitScale.toDouble())

    // 2. matrix.postTranslate(fitOffsetX, fitOffsetY)
    transform = CGAffineTransformTranslate(transform, fitOffsetX.toDouble(), fitOffsetY.toDouble())

    // 3. matrix.postScale(zoomScaleFit, zoomScaleFit, centerX, centerY)
    // Equivalent: translate(centerX, centerY) -> scale(zoom, zoom) -> translate(-centerX, -centerY)
    transform = CGAffineTransformConcat(transform, CGAffineTransformMakeTranslation(centerX.toDouble(), centerY.toDouble()))
    transform = CGAffineTransformScale(transform, zoomScaleFit.toDouble(), zoomScaleFit.toDouble())
    transform = CGAffineTransformTranslate(transform, -centerX.toDouble(), -centerY.toDouble())

    // 4. matrix.postTranslate(offsetXFit, offsetYFit)
    transform = CGAffineTransformTranslate(transform, offsetXFit.toDouble(), offsetYFit.toDouble())

    // 5. matrix.postScale(scaleRatio, scaleRatio)
    transform = CGAffineTransformScale(transform, scaleRatio.toDouble(), scaleRatio.toDouble())

    // 6. matrix.postTranslate(...)
    val finalDx = outputTransform.offsetX - uiTransform.offsetX * scaleRatio
    val finalDy = outputTransform.offsetY - uiTransform.offsetY * scaleRatio
    transform = CGAffineTransformTranslate(transform, finalDx.toDouble(), finalDy.toDouble())

    return transform
}