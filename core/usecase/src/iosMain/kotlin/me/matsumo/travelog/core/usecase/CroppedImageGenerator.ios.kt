@file:Suppress("UnusedPrivateMember", "FunctionName")
@file:OptIn(ExperimentalForeignApi::class)

package me.matsumo.travelog.core.usecase

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.db.CropData
import me.matsumo.travelog.core.model.geo.GeoArea
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFNumberGetValue
import platform.CoreFoundation.CFNumberRef
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreGraphics.CGAffineTransform
import platform.CoreGraphics.CGAffineTransformMake
import platform.CoreGraphics.CGContextAddPath
import platform.CoreGraphics.CGContextClip
import platform.CoreGraphics.CGContextConcatCTM
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGContextSetAllowsAntialiasing
import platform.CoreGraphics.CGContextSetInterpolationQuality
import platform.CoreGraphics.CGContextSetShouldAntialias
import platform.CoreGraphics.CGImageCreateWithImageInRect
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGPathAddLineToPoint
import platform.CoreGraphics.CGPathCloseSubpath
import platform.CoreGraphics.CGPathCreateMutable
import platform.CoreGraphics.CGPathMoveToPoint
import platform.CoreGraphics.CGPathRef
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSLog
import platform.Foundation.dataWithBytes
import platform.ImageIO.CGImageSourceCopyPropertiesAtIndex
import platform.ImageIO.CGImageSourceCreateImageAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.kCGImagePropertyOrientation
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageOrientation
import platform.posix.memcpy
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

actual suspend fun generateCroppedImage(
    imageBytes: ByteArray,
    geoArea: GeoArea,
    cropData: CropData,
    outputSize: Int,
): ByteArray = withContext(Dispatchers.Default) {
    val image = decodeUIImageWithExif(imageBytes) ?: run {
        logE("Failed to decode image. Returning original bytes.")
        return@withContext imageBytes
    }

    val cg = image.CGImage ?: run {
        logE("Decoded UIImage has no CGImage. Returning original bytes.")
        return@withContext imageBytes
    }
    val bitmapWidth = CGImageGetWidth(cg).toInt()
    val bitmapHeight = CGImageGetHeight(cg).toInt()

    val areas = geoArea.children.takeIf { it.isNotEmpty() } ?: listOf(geoArea)
    val bounds = calculateBounds(areas) ?: run {
        logE("Geo bounds not found. Returning original bytes.")
        return@withContext imageBytes
    }

    val viewWidth = cropData.viewWidth.takeIf { it > 0f } ?: outputSize.toFloat()
    val viewHeight = cropData.viewHeight.takeIf { it > 0f } ?: outputSize.toFloat()
    val uiPadding = cropData.viewportPadding.takeIf { it > 0f } ?: DEFAULT_UI_PADDING

    if (cropData.viewWidth <= 0f || cropData.viewHeight <= 0f) {
        logW("Missing view size in cropData. Fallback to outputSize=$outputSize.")
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

    val fitScale = calculateFitScale(viewWidth, viewHeight, bitmapWidth, bitmapHeight)
    val cropScale = calculateCropScale(viewWidth, viewHeight, bitmapWidth, bitmapHeight)
    val zoomScaleFit = if (fitScale > 0f) cropData.scale * (cropScale / fitScale) else cropData.scale
    val offsetXFit = cropData.offsetX * viewWidth
    val offsetYFit = cropData.offsetY * viewHeight

    logD(
        "Crop params: image=${bitmapWidth}x${bitmapHeight}, view=${viewWidth}x${viewHeight}, " +
                "fitScale=$fitScale, cropScale=$cropScale, zoomScaleFit=$zoomScaleFit, " +
                "offsetFit=($offsetXFit,$offsetYFit), uiTransform=$uiTransform, " +
                "outputTransform=$outputTransform, scaleRatio=$scaleRatio",
    )

    val imageTransform = buildImageTransform(
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

    val clipPath = createGeoPath(areas, bounds, outputTransform) ?: run {
        logE("Failed to create geo path. Returning original bytes.")
        return@withContext imageBytes
    }

    // --- Render output (outputSize x outputSize), clip -> draw image with transform ---
    val outputImage: UIImage = run {
        UIGraphicsBeginImageContextWithOptions(
            CGSizeMake(outputSize.toDouble(), outputSize.toDouble()),
            false,
            1.0,
        )
        val ctx = UIGraphicsGetCurrentContext()
        if (ctx == null) {
            UIGraphicsEndImageContext()
            logE("UIGraphicsGetCurrentContext() returned null.")
            return@withContext imageBytes
        }

        CGContextSetInterpolationQuality(ctx, platform.CoreGraphics.kCGInterpolationHigh)
        CGContextSetAllowsAntialiasing(ctx, true)
        CGContextSetShouldAntialias(ctx, true)

        CGContextSaveGState(ctx)
        CGContextAddPath(ctx, clipPath)
        CGContextClip(ctx)

        CGContextConcatCTM(ctx, imageTransform)

        // ここで (0,0)-(bitmapWidth,bitmapHeight) を「AndroidのBitmap座標」として描く。
        image.drawInRect(
            CGRectMake(
                0.0, 0.0,
                bitmapWidth.toDouble(), bitmapHeight.toDouble()
            )
        )

        CGContextRestoreGState(ctx)

        val out = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        out ?: run {
            logE("Failed to get output image from context.")
            return@withContext imageBytes
        }
    }

    val outputCg = outputImage.CGImage ?: run {
        logE("Rendered output UIImage has no CGImage.")
        return@withContext imageBytes
    }

    // --- Compute crop rect (same as Android) ---
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

    logD(
        "Output content rect: left=$cropLeft top=$cropTop width=$cropWidth height=$cropHeight " +
                "outputSize=$outputSize",
    )

    val croppedCg = run {
        val rect = CGRectMake(
            cropLeft.toDouble(),
            cropTop.toDouble(),
            cropWidth.toDouble(),
            cropHeight.toDouble(),
        )
        CGImageCreateWithImageInRect(outputCg, rect)
    }

    val croppedImage = if (croppedCg != null) {
        UIImage.imageWithCGImage(croppedCg, 1.0, UIImageOrientation.UIImageOrientationUp)
    } else {
        logW("Failed to crop output CGImage. Using full output.")
        outputImage
    }

    val finalImage =
        if ((croppedImage.CGImage?.let { CGImageGetWidth(it).toInt() } ?: outputSize) != outputSize ||
            (croppedImage.CGImage?.let { CGImageGetHeight(it).toInt() } ?: outputSize) != outputSize
        ) {
            scaleToSquare(croppedImage, outputSize)
        } else {
            croppedImage
        }

    val jpeg = UIImageJPEGRepresentation(finalImage, JPEG_QUALITY_F)
    jpeg?.toByteArray() ?: run {
        logW("UIImageJPEGRepresentation returned null. Returning original bytes.")
        imageBytes
    }
}

private const val DEFAULT_UI_PADDING = 0.1f
private const val OUTPUT_PADDING = 0f

// Android: 92/100。iOS は 0.0..1.0
private const val JPEG_QUALITY_F = 0.92

// ---- Logging ----
private fun logD(msg: String) = NSLog("CroppedImageGenerator D: %s", msg)
private fun logW(msg: String) = NSLog("CroppedImageGenerator W: %s", msg)
private fun logE(msg: String) = NSLog("CroppedImageGenerator E: %s", msg)

// ---- EXIF decode & normalize (match Android decodeBitmapWithExif) ----
@Suppress("UNCHECKED_CAST")
private fun decodeUIImageWithExif(bytes: ByteArray): UIImage? {
    val data = bytes.toNSData()
    val cfData = CFBridgingRetain(data) as? CFDataRef ?: return null
    val source = CGImageSourceCreateWithData(cfData, null) ?: return null
    val cgImage = CGImageSourceCreateImageAtIndex(source, 0u, null) ?: return null

    val propsAny = CGImageSourceCopyPropertiesAtIndex(source, 0u, null)
    val orientationValue = if (propsAny != null) {
        memScoped {
            val intVar = alloc<IntVar>()
            val orientationRef = CFDictionaryGetValue(propsAny, kCGImagePropertyOrientation)
            @Suppress("UNCHECKED_CAST")
            if (orientationRef != null && CFNumberGetValue(orientationRef as CFNumberRef, kCFNumberIntType, intVar.ptr)) {
                intVar.value
            } else {
                1
            }
        }
    } else {
        1
    }

    val uiOrientation = exifToUIImageOrientation(orientationValue)
    val withOrientation = UIImage.imageWithCGImage(cgImage, 1.0, uiOrientation)

    // UIKit の orientation を「ピクセルに焼き込んで .up」に正規化（Androidのmatrix適用に合わせる）
    return normalizeOrientation(withOrientation)
}

private fun exifToUIImageOrientation(exif: Int): UIImageOrientation = when (exif) {
    1 -> UIImageOrientation.UIImageOrientationUp
    2 -> UIImageOrientation.UIImageOrientationUpMirrored
    3 -> UIImageOrientation.UIImageOrientationDown
    4 -> UIImageOrientation.UIImageOrientationDownMirrored
    5 -> UIImageOrientation.UIImageOrientationLeftMirrored
    6 -> UIImageOrientation.UIImageOrientationRight
    7 -> UIImageOrientation.UIImageOrientationRightMirrored
    8 -> UIImageOrientation.UIImageOrientationLeft
    else -> UIImageOrientation.UIImageOrientationUp
}

private fun normalizeOrientation(image: UIImage): UIImage {
    if (image.imageOrientation == UIImageOrientation.UIImageOrientationUp) return image
    val cg = image.CGImage ?: return image

    val isSwapWH =
        image.imageOrientation == UIImageOrientation.UIImageOrientationLeft ||
                image.imageOrientation == UIImageOrientation.UIImageOrientationRight ||
                image.imageOrientation == UIImageOrientation.UIImageOrientationLeftMirrored ||
                image.imageOrientation == UIImageOrientation.UIImageOrientationRightMirrored

    val w = CGImageGetWidth(cg).toInt()
    val h = CGImageGetHeight(cg).toInt()
    val outW = if (isSwapWH) h else w
    val outH = if (isSwapWH) w else h

    UIGraphicsBeginImageContextWithOptions(CGSizeMake(outW.toDouble(), outH.toDouble()), false, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, outW.toDouble(), outH.toDouble()))
    val normalized = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    return normalized ?: image
}

private fun scaleToSquare(image: UIImage, size: Int): UIImage {
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(size.toDouble(), size.toDouble()), false, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, size.toDouble(), size.toDouble()))
    val scaled = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return scaled ?: image
}

// ---- Geometry helpers (same as Android) ----
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

    return if (hasCoordinates) Bounds(minLon, maxLon, minLat, maxLat) else null
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

private fun createGeoPath(
    areas: List<GeoArea>,
    bounds: Bounds,
    transform: ViewportTransform,
): CGPathRef? {
    val path = CGPathCreateMutable() ?: return null
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

// ---- Android Matrix と同じ「post」順序で合成する（row-vector前提） ----
private class AffineM(
    var a: Float = 1f,
    var b: Float = 0f,
    var c: Float = 0f,
    var d: Float = 1f,
    var tx: Float = 0f,
    var ty: Float = 0f,
) {
    fun setScale(sx: Float, sy: Float) {
        a = sx; b = 0f
        c = 0f; d = sy
        tx = 0f; ty = 0f
    }

    /**
     * Android Matrix の postTranslate と同じ: M = M * T(dx,dy)
     */
    fun postTranslate(dx: Float, dy: Float) {
        // tx' = a*dx + c*dy + tx
        // ty' = b*dx + d*dy + ty
        tx += a * dx + c * dy
        ty += b * dx + d * dy
    }

    /**
     * Android Matrix の postScale と同じ: M = M * S(sx,sy)
     * 注意: translation はスケールしない
     */
    fun postScale(sx: Float, sy: Float) {
        a *= sx
        b *= sx
        c *= sy
        d *= sy
        // tx, ty はそのまま
    }

    fun postScalePivot(sx: Float, sy: Float, px: Float, py: Float) {
        // M = M * T(px,py) * S(sx,sy) * T(-px,-py)
        postTranslate(px, py)
        postScale(sx, sy)
        postTranslate(-px, -py)
    }

    fun toCGAffineTransform(): CValue<CGAffineTransform> = CGAffineTransformMake(
        a.toDouble(),
        b.toDouble(),
        c.toDouble(),
        d.toDouble(),
        tx.toDouble(),
        ty.toDouble(),
    )
}


private fun buildImageTransform(
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
): CValue<CGAffineTransform> {
    val m = AffineM()

    val fitOffsetX = (viewWidth - bitmapWidth * fitScale) / 2f
    val fitOffsetY = (viewHeight - bitmapHeight * fitScale) / 2f
    val centerX = viewWidth / 2f
    val centerY = viewHeight / 2f

    m.setScale(fitScale, fitScale)
    m.postTranslate(fitOffsetX, fitOffsetY)
    m.postScalePivot(zoomScaleFit, zoomScaleFit, centerX, centerY)
    m.postTranslate(offsetXFit, offsetYFit)

    m.postScale(scaleRatio, scaleRatio)
    m.postTranslate(
        outputTransform.offsetX - uiTransform.offsetX * scaleRatio,
        outputTransform.offsetY - uiTransform.offsetY * scaleRatio,
    )

    return m.toCGAffineTransform()
}

// ---- ByteArray <-> NSData ----
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }

private fun NSData.toByteArray(): ByteArray {
    val len = length.toInt()
    val out = ByteArray(len)
    out.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, len.toULong())
    }
    return out
}
