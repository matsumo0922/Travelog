package me.matsumo.travelog.core.usecase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.db.CropData
import me.matsumo.travelog.core.model.geo.GeoArea
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

actual suspend fun generateCroppedImage(
    imageBytes: ByteArray,
    geoArea: GeoArea,
    cropData: CropData,
    outputSize: Int,
): ByteArray = withContext(Dispatchers.Default) {
    val bitmap = decodeBitmapWithExif(imageBytes) ?: run {
        Log.e(TAG, "Failed to decode bitmap. Returning original bytes.")
        return@withContext imageBytes
    }

    val areas = geoArea.children.takeIf { it.isNotEmpty() } ?: listOf(geoArea)
    val bounds = calculateBounds(areas) ?: run {
        Log.e(TAG, "Geo bounds not found. Returning original bytes.")
        return@withContext imageBytes
    }

    val viewWidth = cropData.viewWidth.takeIf { it > 0f } ?: outputSize.toFloat()
    val viewHeight = cropData.viewHeight.takeIf { it > 0f } ?: outputSize.toFloat()
    val uiPadding = cropData.viewportPadding.takeIf { it > 0f } ?: DEFAULT_UI_PADDING

    if (cropData.viewWidth <= 0f || cropData.viewHeight <= 0f) {
        Log.w(TAG, "Missing view size in cropData. Fallback to outputSize=$outputSize.")
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

    val fitScale = calculateFitScale(viewWidth, viewHeight, bitmap.width, bitmap.height)
    val cropScale = calculateCropScale(viewWidth, viewHeight, bitmap.width, bitmap.height)
    val zoomScaleFit = if (fitScale > 0f) cropData.scale * (cropScale / fitScale) else cropData.scale
    val offsetXFit = cropData.offsetX * viewWidth
    val offsetYFit = cropData.offsetY * viewHeight

    Log.d(
        TAG,
        "Crop params: image=${bitmap.width}x${bitmap.height}, view=${viewWidth}x${viewHeight}, " +
                "fitScale=$fitScale, cropScale=$cropScale, zoomScaleFit=$zoomScaleFit, " +
                "offsetFit=($offsetXFit,$offsetYFit), uiTransform=$uiTransform, " +
                "outputTransform=$outputTransform, scaleRatio=$scaleRatio",
    )

    val outputBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(outputBitmap)

    val matrix = buildImageMatrix(
        bitmapWidth = bitmap.width,
        bitmapHeight = bitmap.height,
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

    val clipPath = createGeoPath(areas, bounds, outputTransform)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.save()
    canvas.clipPath(clipPath)
    canvas.drawBitmap(bitmap, matrix, paint)
    canvas.restore()

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

    Log.d(
        TAG,
        "Output content rect: left=$cropLeft top=$cropTop width=$cropWidth height=$cropHeight " +
                "outputSize=$outputSize",
    )

    val croppedBitmap = try {
        Bitmap.createBitmap(outputBitmap, cropLeft, cropTop, cropWidth, cropHeight)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to crop output bitmap: ${e.message}")
        outputBitmap
    }

    val finalBitmap = if (croppedBitmap.width != outputSize || croppedBitmap.height != outputSize) {
        croppedBitmap.scale(outputSize, outputSize)
    } else {
        croppedBitmap
    }

    val outStream = ByteArrayOutputStream()
    finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outStream)
    outStream.toByteArray()
}

private const val TAG = "CroppedImageGenerator"
private const val DEFAULT_UI_PADDING = 0.1f
private const val OUTPUT_PADDING = 0f
private const val JPEG_QUALITY = 92

private fun decodeBitmapWithExif(bytes: ByteArray): Bitmap? {
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

    return try {
        val exif = ExifInterface(ByteArrayInputStream(bytes))
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }

            else -> {
                return decoded
            }
        }
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to apply EXIF orientation: ${e.message}")
        decoded
    }
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
    return kotlin.math.ln(kotlin.math.tan(PI / 4.0 + latRad / 2.0)) * 180.0 / PI
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
): Path {
    val path = Path()
    val maxLatMerc = latToMercator(bounds.maxLat)

    areas.forEach { area ->
        area.polygons.forEach { polygon ->
            polygon.forEach { ring ->
                if (ring.isEmpty()) return@forEach
                val first = ring.first()
                val startX = transform.offsetX + ((first.lon - bounds.minLon) * transform.scale).toFloat()
                val startY = transform.offsetY + ((maxLatMerc - latToMercator(first.lat)) * transform.scale).toFloat()
                path.moveTo(startX, startY)

                ring.drop(1).forEach { coordinate ->
                    val x = transform.offsetX + ((coordinate.lon - bounds.minLon) * transform.scale).toFloat()
                    val y = transform.offsetY + ((maxLatMerc - latToMercator(coordinate.lat)) * transform.scale).toFloat()
                    path.lineTo(x, y)
                }
                path.close()
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
): Matrix {
    val matrix = Matrix()

    val fitOffsetX = (viewWidth - bitmapWidth * fitScale) / 2f
    val fitOffsetY = (viewHeight - bitmapHeight * fitScale) / 2f
    val centerX = viewWidth / 2f
    val centerY = viewHeight / 2f

    matrix.setScale(fitScale, fitScale)
    matrix.postTranslate(fitOffsetX, fitOffsetY)
    matrix.postScale(zoomScaleFit, zoomScaleFit, centerX, centerY)
    matrix.postTranslate(offsetXFit, offsetYFit)

    matrix.postScale(scaleRatio, scaleRatio)
    matrix.postTranslate(
        outputTransform.offsetX - uiTransform.offsetX * scaleRatio,
        outputTransform.offsetY - uiTransform.offsetY * scaleRatio,
    )

    return matrix
}
