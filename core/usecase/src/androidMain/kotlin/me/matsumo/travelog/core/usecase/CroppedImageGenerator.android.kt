package me.matsumo.travelog.core.usecase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Path
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.db.CropData
import me.matsumo.travelog.core.model.geo.GeoArea
import java.io.ByteArrayOutputStream
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
    // Decode source image
    val sourceBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        ?: throw IllegalArgumentException("Failed to decode image")

    // Get areas (either children or self)
    val areas = geoArea.children.takeIf { it.isNotEmpty() } ?: listOf(geoArea)

    // Calculate bounds
    val bounds = calculateBounds(areas) ?: throw IllegalArgumentException("No coordinates found")

    // Calculate output dimensions based on polygon's aspect ratio (using Mercator projection)
    val minLatMerc = latToMercator(bounds.minLat)
    val maxLatMerc = latToMercator(bounds.maxLat)
    val latRangeMerc = maxLatMerc - minLatMerc
    val lonRange = bounds.maxLon - bounds.minLon

    val aspectRatio = lonRange / latRangeMerc
    val outputWidth: Int
    val outputHeight: Int
    if (aspectRatio > 1.0) {
        // Wider than tall
        outputWidth = outputSize
        outputHeight = (outputSize / aspectRatio).toInt().coerceAtLeast(1)
    } else {
        // Taller than wide
        outputHeight = outputSize
        outputWidth = (outputSize * aspectRatio).toInt().coerceAtLeast(1)
    }

    // Create output bitmap with correct aspect ratio
    val outputBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(outputBitmap)

    // Calculate viewport transform (no padding, polygon fills entire image)
    val transform = calculateViewportTransform(bounds, outputWidth.toFloat(), outputHeight.toFloat(), padding = 0f)

    // Create polygon path
    val clipPath = createPolygonPath(areas, bounds, transform)

    // Draw clipped image
    canvas.save()
    canvas.clipPath(clipPath)

    // Apply crop transform
    val scale = cropData.scale
    val offsetX = cropData.offsetX * outputWidth
    val offsetY = cropData.offsetY * outputHeight

    // Calculate source rect to destination rect mapping
    val imageAspect = sourceBitmap.width.toFloat() / sourceBitmap.height
    val canvasAspect = outputWidth.toFloat() / outputHeight

    val drawWidth: Float
    val drawHeight: Float
    if (imageAspect > canvasAspect) {
        drawHeight = outputHeight.toFloat()
        drawWidth = drawHeight * imageAspect
    } else {
        drawWidth = outputWidth.toFloat()
        drawHeight = drawWidth / imageAspect
    }

    val centerX = outputWidth / 2f
    val centerY = outputHeight / 2f

    canvas.translate(centerX + offsetX, centerY + offsetY)
    canvas.scale(scale, scale)
    canvas.translate(-drawWidth / 2f, -drawHeight / 2f)

    val scaledBitmap = sourceBitmap.scale(drawWidth.toInt(), drawHeight.toInt())
    canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
    canvas.restore()

    // Clean up
    sourceBitmap.recycle()
    scaledBitmap.recycle()

    // Encode to JPEG
    val outputStream = ByteArrayOutputStream()
    outputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    outputBitmap.recycle()

    outputStream.toByteArray()
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

private fun createPolygonPath(
    areas: List<GeoArea>,
    bounds: Bounds,
    transform: ViewportTransform,
): Path {
    val combinedPath = Path()
    val maxLatMerc = latToMercator(bounds.maxLat)

    areas.forEach { area ->
        area.polygons.forEach { polygon ->
            polygon.forEach { ring ->
                if (ring.isEmpty()) return@forEach

                val path = Path()
                val first = ring.first()
                val startX = transform.offsetX + ((first.lon - bounds.minLon) * transform.scale).toFloat()
                val startY = transform.offsetY + ((maxLatMerc - latToMercator(first.lat)) * transform.scale).toFloat()
                path.moveTo(startX, startY)

                ring.drop(1).forEach { coord ->
                    val x = transform.offsetX + ((coord.lon - bounds.minLon) * transform.scale).toFloat()
                    val y = transform.offsetY + ((maxLatMerc - latToMercator(coord.lat)) * transform.scale).toFloat()
                    path.lineTo(x, y)
                }
                path.close()

                combinedPath.addPath(path)
            }
        }
    }

    return combinedPath
}
