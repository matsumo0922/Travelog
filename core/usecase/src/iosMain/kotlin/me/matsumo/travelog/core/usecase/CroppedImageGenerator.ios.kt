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
import platform.CoreGraphics.CGAffineTransformMake
import platform.CoreGraphics.CGAffineTransformScale
import platform.CoreGraphics.CGAffineTransformTranslate
import platform.CoreGraphics.CGContextAddPath
import platform.CoreGraphics.CGContextClip
import platform.CoreGraphics.CGContextConcatCTM
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGImageCreateWithImageInRect
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGPathAddLineToPoint
import platform.CoreGraphics.CGPathCloseSubpath
import platform.CoreGraphics.CGPathCreateMutable
import platform.CoreGraphics.CGPathMoveToPoint
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSLog
import platform.Foundation.create
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIScreen
import platform.posix.memcpy
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun generateCroppedImage(
    imageBytes: ByteArray,
    geoArea: GeoArea,
    cropData: CropData,
    outputSize: Int,
): ByteArray = withContext(Dispatchers.IO) {
    val nsData = imageBytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = imageBytes.size.toULong())
    }

    val sourceImage = UIImage.imageWithData(nsData)
    if (sourceImage == null) {
        NSLog("CroppedImageGenerator.ios: failed to decode image")
        return@withContext imageBytes
    }

    val normalizedImage = normalizeImage(sourceImage) ?: sourceImage
    val cgImage = normalizedImage.CGImage ?: run {
        NSLog("CroppedImageGenerator.ios: missing CGImage")
        return@withContext imageBytes
    }

    val imageWidth = CGImageGetWidth(cgImage).toInt()
    val imageHeight = CGImageGetHeight(cgImage).toInt()

    val areas = geoArea.children.takeIf { it.isNotEmpty() } ?: listOf(geoArea)
    val bounds = calculateBounds(areas) ?: run {
        NSLog("CroppedImageGenerator.ios: geo bounds not found")
        return@withContext imageBytes
    }

    val rawViewWidth = if (cropData.viewWidth > 0f) cropData.viewWidth.toDouble() else outputSize.toDouble()
    val rawViewHeight = if (cropData.viewHeight > 0f) cropData.viewHeight.toDouble() else outputSize.toDouble()
    val screenScale = UIScreen.mainScreen.scale
    val (screenWidthPoints, screenHeightPoints) = UIScreen.mainScreen.bounds.useContents {
        size.width to size.height
    }
    val screenWidthPixels = screenWidthPoints * screenScale
    val screenHeightPixels = screenHeightPoints * screenScale
    val isPointUnitWidth = kotlin.math.abs(rawViewWidth - screenWidthPoints) < kotlin.math.abs(rawViewWidth - screenWidthPixels)
    val isPointUnitHeight = kotlin.math.abs(rawViewHeight - screenHeightPoints) < kotlin.math.abs(rawViewHeight - screenHeightPixels)
    val isPointUnit = isPointUnitWidth || isPointUnitHeight
    val viewWidth = if (isPointUnit) rawViewWidth * screenScale else rawViewWidth
    val viewHeight = if (isPointUnit) rawViewHeight * screenScale else rawViewHeight
    val uiPadding = if (cropData.viewportPadding > 0f) cropData.viewportPadding.toDouble() else DEFAULT_UI_PADDING

    if (cropData.viewWidth <= 0f || cropData.viewHeight <= 0f) {
        NSLog("CroppedImageGenerator.ios: missing view size, fallback to outputSize=$outputSize")
    }

    val uiTransform = calculateViewportTransform(
        bounds = bounds,
        canvasWidth = viewWidth,
        canvasHeight = viewHeight,
        padding = uiPadding,
    )
    val outputTransform = calculateViewportTransform(
        bounds = bounds,
        canvasWidth = outputSize.toDouble(),
        canvasHeight = outputSize.toDouble(),
        padding = OUTPUT_PADDING,
    )
    val scaleRatio = if (uiTransform.scale > 0.0) outputTransform.scale / uiTransform.scale else 1.0

    val fitScale = calculateFitScale(viewWidth, viewHeight, imageWidth, imageHeight)
    val cropScale = calculateCropScale(viewWidth, viewHeight, imageWidth, imageHeight)
    val zoomScaleFit = if (fitScale > 0.0) cropData.scale.toDouble() * (cropScale / fitScale) else cropData.scale.toDouble()
    val offsetXFit = cropData.offsetX.toDouble() * viewWidth
    val offsetYFit = cropData.offsetY.toDouble() * viewHeight

    NSLog(
        "Crop params: image=${imageWidth}x${imageHeight}, view=${viewWidth}x${viewHeight}, " +
                "rawView=${rawViewWidth}x${rawViewHeight}, screenScale=$screenScale, " +
                "screen=${screenWidthPoints}x${screenHeightPoints}pt " +
                "(${screenWidthPixels}x${screenHeightPixels}px), " +
                "isPointUnit=$isPointUnit, " +
                "fitScale=$fitScale, cropScale=$cropScale, zoomScaleFit=$zoomScaleFit, " +
                "offsetFit=($offsetXFit,$offsetYFit), uiTransform=$uiTransform, " +
                "outputTransform=$outputTransform, scaleRatio=$scaleRatio",
    )

    UIGraphicsBeginImageContextWithOptions(CGSizeMake(outputSize.toDouble(), outputSize.toDouble()), false, 1.0)
    val context = UIGraphicsGetCurrentContext() ?: run {
        UIGraphicsEndImageContext()
        NSLog("CroppedImageGenerator.ios: failed to get CGContext")
        return@withContext imageBytes
    }

    val clipPath = createGeoPath(areas, bounds, outputTransform)

    CGContextSaveGState(context)
    CGContextAddPath(context, clipPath)
    CGContextClip(context)

    val matrix = buildImageTransform(
        bitmapWidth = imageWidth.toDouble(),
        bitmapHeight = imageHeight.toDouble(),
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

    CGContextConcatCTM(context, matrix)
    normalizedImage.drawInRect(CGRectMake(0.0, 0.0, imageWidth.toDouble(), imageHeight.toDouble()))
    CGContextRestoreGState(context)

    val outputImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    if (outputImage == null) {
        NSLog("CroppedImageGenerator.ios: failed to create output image")
        return@withContext imageBytes
    }

    val maxLatMerc = latToMercator(bounds.maxLat)
    val minLatMerc = latToMercator(bounds.minLat)
    val contentWidth = bounds.lonRange * outputTransform.scale
    val contentHeight = (maxLatMerc - minLatMerc) * outputTransform.scale
    val contentLeft = outputTransform.offsetX
    val contentTop = outputTransform.offsetY

    val cropLeft = contentLeft.coerceIn(0.0, outputSize.toDouble() - 1)
    val cropTop = contentTop.coerceIn(0.0, outputSize.toDouble() - 1)
    val cropRight = (contentLeft + contentWidth).coerceIn(cropLeft + 1.0, outputSize.toDouble())
    val cropBottom = (contentTop + contentHeight).coerceIn(cropTop + 1.0, outputSize.toDouble())
    val cropWidth = cropRight - cropLeft
    val cropHeight = cropBottom - cropTop

    NSLog(
        "Output content rect: left=$cropLeft top=$cropTop width=$cropWidth height=$cropHeight " +
                "outputSize=$outputSize",
    )

    val outputCgImage = outputImage.CGImage ?: run {
        NSLog("CroppedImageGenerator.ios: failed to get output CGImage")
        return@withContext imageBytes
    }

    val croppedCgImage = CGImageCreateWithImageInRect(
        outputCgImage,
        CGRectMake(cropLeft, cropTop, cropWidth, cropHeight),
    ) ?: outputCgImage

    val croppedImage = UIImage.imageWithCGImage(croppedCgImage)

    val (croppedWidth, croppedHeight) = croppedImage.size.useContents {
        width to height
    }
    val finalImage = if (croppedWidth != outputSize.toDouble() || croppedHeight != outputSize.toDouble()) {
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(outputSize.toDouble(), outputSize.toDouble()), false, 1.0)
        croppedImage.drawInRect(CGRectMake(0.0, 0.0, outputSize.toDouble(), outputSize.toDouble()))
        val resized = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        resized ?: croppedImage
    } else {
        croppedImage
    }

    val jpegData = UIImageJPEGRepresentation(finalImage, JPEG_QUALITY) ?: run {
        NSLog("CroppedImageGenerator.ios: failed to encode JPEG")
        return@withContext imageBytes
    }

    jpegData.toByteArray()
}

private const val DEFAULT_UI_PADDING = 0.1
private const val OUTPUT_PADDING = 0.0
private const val JPEG_QUALITY = 0.92

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size <= 0) return ByteArray(0)
    val array = ByteArray(size)
    array.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, size.toULong())
    }
    return array
}

private fun normalizeImage(image: UIImage): UIImage? {
    val cgImage = image.CGImage ?: return image
    val width = CGImageGetWidth(cgImage).toDouble()
    val height = CGImageGetHeight(cgImage).toDouble()
    if (width <= 0.0 || height <= 0.0) return image
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), false, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, width, height))
    val normalized = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return normalized
}

private data class Bounds(
    val minLon: Double,
    val maxLon: Double,
    val minLat: Double,
    val maxLat: Double,
) {
    val lonRange: Double get() = maxLon - minLon
    val latRange: Double get() = maxLat - minLat
}

private data class ViewportTransform(
    val scale: Double,
    val offsetX: Double,
    val offsetY: Double,
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
    return kotlin.math.ln(kotlin.math.tan(PI / 4.0 + latRad / 2.0)) * 180.0 / PI
}

private fun calculateViewportTransform(
    bounds: Bounds,
    canvasWidth: Double,
    canvasHeight: Double,
    padding: Double,
): ViewportTransform {
    val paddedWidth = canvasWidth * (1.0 - padding * 2.0)
    val paddedHeight = canvasHeight * (1.0 - padding * 2.0)

    val minLatMerc = latToMercator(bounds.minLat)
    val maxLatMerc = latToMercator(bounds.maxLat)
    val latRangeMerc = maxLatMerc - minLatMerc

    val scaleX = paddedWidth / bounds.lonRange
    val scaleY = paddedHeight / latRangeMerc
    val scale = min(scaleX, scaleY)

    val contentWidth = bounds.lonRange * scale
    val contentHeight = latRangeMerc * scale
    val offsetX = (canvasWidth - contentWidth) / 2.0
    val offsetY = (canvasHeight - contentHeight) / 2.0

    return ViewportTransform(scale, offsetX, offsetY)
}

private fun createGeoPath(
    areas: List<GeoArea>,
    bounds: Bounds,
    transform: ViewportTransform,
): platform.CoreGraphics.CGMutablePathRef? {
    val path = CGPathCreateMutable()
    val maxLatMerc = latToMercator(bounds.maxLat)

    areas.forEach { area ->
        area.polygons.forEach { polygon ->
            polygon.forEach { ring ->
                if (ring.isEmpty()) return@forEach
                val first = ring.first()
                val startX = transform.offsetX + (first.lon - bounds.minLon) * transform.scale
                val startY = transform.offsetY + (maxLatMerc - latToMercator(first.lat)) * transform.scale
                CGPathMoveToPoint(path, null, startX, startY)

                ring.drop(1).forEach { coordinate ->
                    val x = transform.offsetX + (coordinate.lon - bounds.minLon) * transform.scale
                    val y = transform.offsetY + (maxLatMerc - latToMercator(coordinate.lat)) * transform.scale
                    CGPathAddLineToPoint(path, null, x, y)
                }
                CGPathCloseSubpath(path)
            }
        }
    }

    return path
}

private fun calculateFitScale(
    viewWidth: Double,
    viewHeight: Double,
    imageWidth: Int,
    imageHeight: Int,
): Double {
    if (viewWidth <= 0.0 || viewHeight <= 0.0 || imageWidth <= 0 || imageHeight <= 0) return 1.0
    val scaleX = viewWidth / imageWidth.toDouble()
    val scaleY = viewHeight / imageHeight.toDouble()
    return min(scaleX, scaleY)
}

private fun calculateCropScale(
    viewWidth: Double,
    viewHeight: Double,
    imageWidth: Int,
    imageHeight: Int,
): Double {
    if (viewWidth <= 0.0 || viewHeight <= 0.0 || imageWidth <= 0 || imageHeight <= 0) return 1.0
    val scaleX = viewWidth / imageWidth.toDouble()
    val scaleY = viewHeight / imageHeight.toDouble()
    return max(scaleX, scaleY)
}

private fun buildImageTransform(
    bitmapWidth: Double,
    bitmapHeight: Double,
    viewWidth: Double,
    viewHeight: Double,
    fitScale: Double,
    zoomScaleFit: Double,
    offsetXFit: Double,
    offsetYFit: Double,
    scaleRatio: Double,
    uiTransform: ViewportTransform,
    outputTransform: ViewportTransform,
): kotlinx.cinterop.CValue<platform.CoreGraphics.CGAffineTransform> {
    var transform = CGAffineTransformMake(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

    val fitOffsetX = (viewWidth - bitmapWidth * fitScale) / 2.0
    val fitOffsetY = (viewHeight - bitmapHeight * fitScale) / 2.0
    val centerX = viewWidth / 2.0
    val centerY = viewHeight / 2.0

    transform = CGAffineTransformScale(transform, fitScale, fitScale)
    transform = CGAffineTransformTranslate(transform, fitOffsetX, fitOffsetY)
    transform = CGAffineTransformTranslate(transform, centerX, centerY)
    transform = CGAffineTransformScale(transform, zoomScaleFit, zoomScaleFit)
    transform = CGAffineTransformTranslate(transform, -centerX, -centerY)
    transform = CGAffineTransformTranslate(transform, offsetXFit, offsetYFit)
    transform = CGAffineTransformScale(transform, scaleRatio, scaleRatio)
    transform = CGAffineTransformTranslate(
        transform,
        outputTransform.offsetX - uiTransform.offsetX * scaleRatio,
        outputTransform.offsetY - uiTransform.offsetY * scaleRatio,
    )

    return transform
}
