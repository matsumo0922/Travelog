package me.matsumo.travelog.core.usecase

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.db.CropData
import me.matsumo.travelog.core.model.geo.GeoArea
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFNumberGetValue
import platform.CoreFoundation.CFNumberRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFNumberSInt32Type
import platform.CoreGraphics.CGAffineTransformConcat
import platform.CoreGraphics.CGAffineTransformMakeRotation
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGAffineTransformMakeTranslation
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextAddPath
import platform.CoreGraphics.CGContextClip
import platform.CoreGraphics.CGContextConcatCTM
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGContextSetInterpolationQuality
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGPathAddLineToPoint
import platform.CoreGraphics.CGPathCloseSubpath
import platform.CoreGraphics.CGPathCreateMutable
import platform.CoreGraphics.CGPathMoveToPoint
import platform.CoreGraphics.CGPathRef
import platform.CoreGraphics.CGPathRelease
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.CoreGraphics.kCGInterpolationHigh
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.NSNumber
import platform.Foundation.create
import platform.ImageIO.CGImageDestinationAddImage
import platform.ImageIO.CGImageDestinationCreateWithData
import platform.ImageIO.CGImageDestinationFinalize
import platform.ImageIO.CGImageSourceCopyPropertiesAtIndex
import platform.ImageIO.CGImageSourceCreateImageAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.kCGImageDestinationLossyCompressionQuality
import platform.ImageIO.kCGImagePropertyOrientation
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

private const val TAG = "CroppedImageGenerator"
private const val DEFAULT_UI_PADDING = 0.1f
private const val OUTPUT_PADDING = 0f
private const val JPEG_QUALITY = 0.92

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun generateCroppedImage(
    imageBytes: ByteArray,
    geoArea: GeoArea,
    cropData: CropData,
    outputSize: Int,
): ByteArray = withContext(Dispatchers.IO) {
    val (cgImage, imageWidth, imageHeight) = decodeCGImageWithExif(imageBytes) ?: run {
        println("$TAG: Failed to decode image. Returning original bytes.")
        return@withContext imageBytes
    }

    try {
        val areas = geoArea.children.takeIf { it.isNotEmpty() } ?: listOf(geoArea)
        val bounds = calculateBounds(areas) ?: run {
            println("$TAG: Geo bounds not found. Returning original bytes.")
            CGImageRelease(cgImage)
            return@withContext imageBytes
        }

        val viewWidth = cropData.viewWidth.takeIf { it > 0f } ?: outputSize.toFloat()
        val viewHeight = cropData.viewHeight.takeIf { it > 0f } ?: outputSize.toFloat()
        val uiPadding = cropData.viewportPadding.takeIf { it > 0f } ?: DEFAULT_UI_PADDING

        if (cropData.viewWidth <= 0f || cropData.viewHeight <= 0f) {
            println("$TAG: Missing view size in cropData. Fallback to outputSize=$outputSize.")
        }

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

        val fitScale = calculateFitScale(viewWidth, viewHeight, imageWidth, imageHeight)
        val cropScale = calculateCropScale(viewWidth, viewHeight, imageWidth, imageHeight)
        val zoomScaleFit = if (fitScale > 0f) cropData.scale * (cropScale / fitScale) else cropData.scale
        val offsetXFit = cropData.offsetX * viewWidth
        val offsetYFit = cropData.offsetY * viewHeight

        println(
            "$TAG: Crop params: image=${imageWidth}x${imageHeight}, view=${viewWidth}x${viewHeight}, " +
                    "fitScale=$fitScale, cropScale=$cropScale, zoomScaleFit=$zoomScaleFit, " +
                    "offsetFit=($offsetXFit,$offsetYFit), rotation=${cropData.rotation}, " +
                    "uiTransform=$uiTransform, outputTransform=$outputTransform, scaleRatio=$scaleRatio"
        )

        val colorSpace = CGColorSpaceCreateDeviceRGB()
        val bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big
        val context = CGBitmapContextCreate(
            data = null,
            width = outputSize.toULong(),
            height = outputSize.toULong(),
            bitsPerComponent = 8u,
            bytesPerRow = (outputSize * 4).toULong(),
            space = colorSpace,
            bitmapInfo = bitmapInfo,
        )
        CGColorSpaceRelease(colorSpace)

        if (context == null) {
            println("$TAG: Failed to create bitmap context.")
            CGImageRelease(cgImage)
            return@withContext imageBytes
        }

        CGContextSetInterpolationQuality(context, kCGInterpolationHigh)

        val transform = buildImageTransform(
            bitmapWidth = imageWidth,
            bitmapHeight = imageHeight,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            fitScale = fitScale,
            zoomScaleFit = zoomScaleFit,
            offsetXFit = offsetXFit,
            offsetYFit = offsetYFit,
            rotationDegrees = cropData.rotation,
            scaleRatio = scaleRatio,
            uiTransform = uiTransform,
            outputTransform = outputTransform,
            outputSize = outputSize,
        )

        val clipPath = createGeoPath(areas, bounds, outputTransform, outputSize)

        CGContextSaveGState(context)
        CGContextAddPath(context, clipPath)
        CGContextClip(context)
        CGContextConcatCTM(context, transform)
        CGContextDrawImage(
            context,
            CGRectMake(0.0, 0.0, imageWidth.toDouble(), imageHeight.toDouble()),
            cgImage,
        )
        CGContextRestoreGState(context)
        CGPathRelease(clipPath)
        CGImageRelease(cgImage)

        val outputImage = CGBitmapContextCreateImage(context)
        CGContextRelease(context)

        if (outputImage == null) {
            println("$TAG: Failed to create output image.")
            return@withContext imageBytes
        }

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

        println(
            "$TAG: Output content rect: left=$cropLeft top=$cropTop width=$cropWidth height=$cropHeight " +
                    "outputSize=$outputSize"
        )

        val croppedImage = cropCGImage(outputImage, cropLeft, cropTop, cropWidth, cropHeight, outputSize)
        CGImageRelease(outputImage)

        val finalImage = if (croppedImage != null) {
            val croppedWidth = CGImageGetWidth(croppedImage).toInt()
            val croppedHeight = CGImageGetHeight(croppedImage).toInt()
            if (croppedWidth != outputSize || croppedHeight != outputSize) {
                val scaled = scaleCGImage(croppedImage, outputSize, outputSize)
                CGImageRelease(croppedImage)
                scaled
            } else {
                croppedImage
            }
        } else {
            null
        }

        if (finalImage == null) {
            println("$TAG: Failed to crop/scale image.")
            return@withContext imageBytes
        }

        val result = encodeToJpeg(finalImage, JPEG_QUALITY)
        CGImageRelease(finalImage)

        result ?: imageBytes
    } catch (e: Exception) {
        println("$TAG: Exception during image processing: ${e.message}")
        imageBytes
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun decodeCGImageWithExif(bytes: ByteArray): Triple<CGImageRef, Int, Int>? {
    val nsData = bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }

    @Suppress("UNCHECKED_CAST")
    val cfData = CFBridgingRetain(nsData) as? platform.CoreFoundation.CFDataRef ?: return null
    val imageSource = CGImageSourceCreateWithData(cfData, null)
    CFRelease(cfData)

    if (imageSource == null) return null

    val cgImage = CGImageSourceCreateImageAtIndex(imageSource, 0u, null)
    if (cgImage == null) {
        CFRelease(imageSource)
        return null
    }

    val originalWidth = CGImageGetWidth(cgImage).toInt()
    val originalHeight = CGImageGetHeight(cgImage).toInt()

    val properties = CGImageSourceCopyPropertiesAtIndex(imageSource, 0u, null)
    CFRelease(imageSource)

    val orientation = if (properties != null) {
        val orientationRef = CFDictionaryGetValue(properties, kCGImagePropertyOrientation)
        val orientationValue = if (orientationRef != null) {
            memScoped {
                val intValue = alloc<IntVar>()
                @Suppress("UNCHECKED_CAST")
                if (CFNumberGetValue(orientationRef as CFNumberRef, kCFNumberSInt32Type, intValue.ptr)) {
                    intValue.value
                } else {
                    1
                }
            }
        } else {
            1
        }
        CFRelease(properties)
        orientationValue
    } else {
        1
    }

    return applyExifOrientation(cgImage, orientation, originalWidth, originalHeight)
}

@OptIn(ExperimentalForeignApi::class)
private fun applyExifOrientation(
    image: CGImageRef,
    orientation: Int,
    width: Int,
    height: Int,
): Triple<CGImageRef, Int, Int>? {
    if (orientation == 1) {
        return Triple(image, width, height)
    }

    val (newWidth, newHeight) = when (orientation) {
        5, 6, 7, 8 -> height to width
        else -> width to height
    }

    val colorSpace = CGColorSpaceCreateDeviceRGB()
    val bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big
    val context = CGBitmapContextCreate(
        data = null,
        width = newWidth.toULong(),
        height = newHeight.toULong(),
        bitsPerComponent = 8u,
        bytesPerRow = (newWidth * 4).toULong(),
        space = colorSpace,
        bitmapInfo = bitmapInfo,
    )
    CGColorSpaceRelease(colorSpace)

    if (context == null) {
        CGImageRelease(image)
        return null
    }

    val transform = when (orientation) {
        2 -> { // Flip horizontal
            CGAffineTransformConcat(
                CGAffineTransformMakeTranslation(newWidth.toDouble(), 0.0),
                CGAffineTransformMakeScale(-1.0, 1.0)
            )
        }

        3 -> { // Rotate 180
            CGAffineTransformConcat(
                CGAffineTransformMakeTranslation(newWidth.toDouble(), newHeight.toDouble()),
                CGAffineTransformMakeRotation(PI)
            )
        }

        4 -> { // Flip vertical
            CGAffineTransformConcat(
                CGAffineTransformMakeTranslation(0.0, newHeight.toDouble()),
                CGAffineTransformMakeScale(1.0, -1.0)
            )
        }

        5 -> { // Transpose
            CGAffineTransformConcat(
                CGAffineTransformMakeRotation(PI / 2),
                CGAffineTransformMakeScale(1.0, -1.0)
            )
        }

        6 -> { // Rotate 90 CW
            CGAffineTransformConcat(
                CGAffineTransformMakeTranslation(newWidth.toDouble(), 0.0),
                CGAffineTransformMakeRotation(PI / 2)
            )
        }

        7 -> { // Transverse
            CGAffineTransformConcat(
                CGAffineTransformMakeTranslation(newWidth.toDouble(), newHeight.toDouble()),
                CGAffineTransformConcat(
                    CGAffineTransformMakeRotation(PI / 2),
                    CGAffineTransformMakeScale(-1.0, 1.0)
                )
            )
        }

        8 -> { // Rotate 270 CW (90 CCW)
            CGAffineTransformConcat(
                CGAffineTransformMakeTranslation(0.0, newHeight.toDouble()),
                CGAffineTransformMakeRotation(-PI / 2)
            )
        }

        else -> CGAffineTransformMakeScale(1.0, 1.0) // Identity transform
    }

    CGContextConcatCTM(context, transform)
    CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), image)
    CGImageRelease(image)

    val rotatedImage = CGBitmapContextCreateImage(context)
    CGContextRelease(context)

    return rotatedImage?.let { Triple(it, newWidth, newHeight) }
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
    outputSize: Int,
): CGPathRef {
    val path = CGPathCreateMutable()
    val maxLatMerc = latToMercator(bounds.maxLat)

    areas.forEach { area ->
        area.polygons.forEach { polygon ->
            polygon.forEach { ring ->
                if (ring.isEmpty()) return@forEach
                val first = ring.first()
                val startX = transform.offsetX + ((first.lon - bounds.minLon) * transform.scale).toFloat()
                // CoreGraphics座標系はY軸が下から上なので、outputSizeから引く
                val startY = outputSize - (transform.offsetY + ((maxLatMerc - latToMercator(first.lat)) * transform.scale).toFloat())
                CGPathMoveToPoint(path, null, startX.toDouble(), startY.toDouble())

                ring.drop(1).forEach { coordinate ->
                    val x = transform.offsetX + ((coordinate.lon - bounds.minLon) * transform.scale).toFloat()
                    val y = outputSize - (transform.offsetY + ((maxLatMerc - latToMercator(coordinate.lat)) * transform.scale).toFloat())
                    CGPathAddLineToPoint(path, null, x.toDouble(), y.toDouble())
                }
                CGPathCloseSubpath(path)
            }
        }
    }

    return path!!
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
private fun buildImageTransform(
    bitmapWidth: Int,
    bitmapHeight: Int,
    viewWidth: Float,
    viewHeight: Float,
    fitScale: Float,
    zoomScaleFit: Float,
    offsetXFit: Float,
    offsetYFit: Float,
    rotationDegrees: Float,
    scaleRatio: Float,
    uiTransform: ViewportTransform,
    outputTransform: ViewportTransform,
    outputSize: Int,
): kotlinx.cinterop.CValue<platform.CoreGraphics.CGAffineTransform> {
    // Android の Matrix 操作を CGAffineTransform に変換
    // Android の座標系は Y が下向き、CoreGraphics は Y が上向き
    // そのため、Y 座標を反転させる処理が必要

    // graphicsLayer applies: Scale → Rotation → Translation
    // Android Matrix の操作を順番に適用:
    // 1. setScale(fitScale, fitScale)
    // 2. postTranslate(fitOffsetX, fitOffsetY)
    // 3. postScale(zoomScaleFit, zoomScaleFit, centerX, centerY)
    // 4. postRotate(rotationDegrees, centerX, centerY) - rotation BEFORE pan
    // 5. postTranslate(offsetXFit, offsetYFit) - pan AFTER rotation (in screen coordinates)
    // 6. postScale(scaleRatio, scaleRatio)
    // 7. postTranslate(outputTransform.offsetX - uiTransform.offsetX * scaleRatio,
    //                  outputTransform.offsetY - uiTransform.offsetY * scaleRatio)

    val fitOffsetX = (viewWidth - bitmapWidth * fitScale) / 2f
    val fitOffsetY = (viewHeight - bitmapHeight * fitScale) / 2f
    val centerX = viewWidth / 2f
    val centerY = viewHeight / 2f

    // 最終的なスケール
    val totalScale = fitScale * zoomScaleFit * scaleRatio

    // 中心を基準としたズームを考慮した移動量
    // zoomScaleFit を centerX, centerY 中心で適用後の位置
    val afterZoomX = centerX + (fitOffsetX - centerX) * zoomScaleFit
    val afterZoomY = centerY + (fitOffsetY - centerY) * zoomScaleFit

    // パンを追加（スクリーン座標系での移動、回転の影響は受けない）
    val afterPanX = afterZoomX + offsetXFit
    val afterPanY = afterZoomY + offsetYFit

    // scaleRatio を適用（原点基準）
    val afterScaleRatioX = afterPanX * scaleRatio
    val afterScaleRatioY = afterPanY * scaleRatio

    // 最終的な移動量を追加
    val finalOffsetX = afterScaleRatioX + outputTransform.offsetX - uiTransform.offsetX * scaleRatio
    val finalOffsetY = afterScaleRatioY + outputTransform.offsetY - uiTransform.offsetY * scaleRatio

    // CoreGraphics は Y 軸が上向きなので、Y を反転
    // 画像を描画するとき、Y 座標を outputSize から引く必要がある

    // 基本変換: Y軸反転 + 移動 + スケール
    var transform = CGAffineTransformMakeTranslation(finalOffsetX.toDouble(), (outputSize - finalOffsetY).toDouble())
    transform = CGAffineTransformConcat(CGAffineTransformMakeScale(totalScale.toDouble(), -totalScale.toDouble()), transform)

    // 回転を適用（CoreGraphics は Y 軸が上向きなので角度を反転）
    // 出力画像の中心を基準に回転
    if (rotationDegrees != 0f) {
        val cgRotationRadians = -rotationDegrees * PI / 180.0
        val outputCenterX = outputSize / 2.0
        val outputCenterY = outputSize / 2.0

        // Translate to origin → Rotate → Translate back
        transform = CGAffineTransformConcat(
            transform,
            CGAffineTransformMakeTranslation(-outputCenterX, -outputCenterY),
        )
        transform = CGAffineTransformConcat(
            transform,
            CGAffineTransformMakeRotation(cgRotationRadians),
        )
        transform = CGAffineTransformConcat(
            transform,
            CGAffineTransformMakeTranslation(outputCenterX, outputCenterY),
        )
    }

    return transform
}

@OptIn(ExperimentalForeignApi::class)
private fun cropCGImage(
    image: CGImageRef,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    outputSize: Int,
): CGImageRef? {
    // CoreGraphics の座標系では Y 軸が反転しているため、Y を調整
    val cgY = outputSize - y - height
    val rect = CGRectMake(x.toDouble(), cgY.toDouble(), width.toDouble(), height.toDouble())
    return platform.CoreGraphics.CGImageCreateWithImageInRect(image, rect)
}

@OptIn(ExperimentalForeignApi::class)
private fun scaleCGImage(image: CGImageRef, targetWidth: Int, targetHeight: Int): CGImageRef? {
    val colorSpace = CGColorSpaceCreateDeviceRGB()
    val bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big
    val context = CGBitmapContextCreate(
        data = null,
        width = targetWidth.toULong(),
        height = targetHeight.toULong(),
        bitsPerComponent = 8u,
        bytesPerRow = (targetWidth * 4).toULong(),
        space = colorSpace,
        bitmapInfo = bitmapInfo,
    )
    CGColorSpaceRelease(colorSpace)

    if (context == null) return null

    CGContextSetInterpolationQuality(context, kCGInterpolationHigh)
    CGContextDrawImage(context, CGRectMake(0.0, 0.0, targetWidth.toDouble(), targetHeight.toDouble()), image)

    val scaledImage = CGBitmapContextCreateImage(context)
    CGContextRelease(context)

    return scaledImage
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun encodeToJpeg(image: CGImageRef, quality: Double): ByteArray? {
    val mutableData = NSMutableData()

    @Suppress("UNCHECKED_CAST")
    val cfData = CFBridgingRetain(mutableData) as? platform.CoreFoundation.CFDataRef

    @Suppress("UNCHECKED_CAST")
    val jpegType = CFBridgingRetain("public.jpeg") as platform.CoreFoundation.CFStringRef

    val destination = CGImageDestinationCreateWithData(
        data = cfData as platform.CoreFoundation.CFMutableDataRef,
        type = jpegType,
        count = 1u,
        options = null,
    )

    if (destination == null) {
        CFRelease(cfData)
        return null
    }

    val options = mapOf(
        kCGImageDestinationLossyCompressionQuality to NSNumber(quality)
    )

    @Suppress("UNCHECKED_CAST")
    val cfOptions = CFBridgingRetain(options) as? platform.CoreFoundation.CFDictionaryRef

    CGImageDestinationAddImage(destination, image, cfOptions)

    val success = CGImageDestinationFinalize(destination)
    CFRelease(destination)
    if (cfOptions != null) CFRelease(cfOptions)

    if (!success) {
        return null
    }

    val length = mutableData.length.toInt()
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), mutableData.bytes, length.toULong())
    }

    return bytes
}
