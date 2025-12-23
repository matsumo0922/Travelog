package me.matsumo.travelog.core.ui.component

import androidx.compose.ui.graphics.Path
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import me.matsumo.travelog.core.model.geo.GeoJsonData
import me.matsumo.travelog.core.model.geo.GeoJsonGeometry
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

/**
 * Helper class for rendering GeoJSON data on Canvas
 */
internal object GeoJsonRenderer {
    data class Bounds(
        val minLon: Double,
        val maxLon: Double,
        val minLat: Double,
        val maxLat: Double,
    ) {
        val lonRange: Double get() = maxLon - minLon
        val latRange: Double get() = maxLat - minLat
    }

    /**
     * Calculate the bounding box of the GeoJSON data
     */
    fun calculateBounds(geoJsonData: GeoJsonData): Bounds? {
        var minLon = Double.MAX_VALUE
        var maxLon = -Double.MAX_VALUE
        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var hasCoordinates = false

        geoJsonData.features.forEach { feature ->
            val coordinates = extractAllCoordinates(feature.geometry)
            coordinates.forEach { (lon, lat) ->
                hasCoordinates = true
                minLon = min(minLon, lon)
                maxLon = max(maxLon, lon)
                minLat = min(minLat, lat)
                maxLat = max(maxLat, lat)
            }
        }

        return if (hasCoordinates) {
            Bounds(minLon, maxLon, minLat, maxLat)
        } else {
            null
        }
    }

    /**
     * Extract all coordinates from geometry
     */
    private fun extractAllCoordinates(geometry: GeoJsonGeometry): List<Pair<Double, Double>> {
        return when (geometry.type) {
            "Polygon" -> {
                parsePolygonCoordinates(geometry.coordinates)?.flatten() ?: emptyList()
            }

            "MultiPolygon" -> {
                parseMultiPolygonCoordinates(geometry.coordinates).flatten().flatten()
            }

            else -> emptyList()
        }
    }


    /**
     * Calculate the scale factor and offset to maintain aspect ratio
     */
    data class ViewportTransform(
        val scale: Float,
        val offsetX: Float,
        val offsetY: Float,
    )

    private fun latToMercator(lat: Double): Double {
        val latRad = lat * PI / 180.0
        return kotlin.math.ln(kotlin.math.tan(PI / 4.0 + latRad / 2.0)) * 180.0 / PI
    }

    /**
     * Calculate viewport transform to maintain aspect ratio
     * Uses Mercator projection for accurate scaling
     */
    fun calculateViewportTransform(
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

        // Calculate scale to fit bounds while maintaining aspect ratio
        val scaleX = paddedWidth / bounds.lonRange.toFloat()
        val scaleY = paddedHeight / latRangeMerc.toFloat()
        val scale = minOf(scaleX, scaleY)

        // Calculate content size with uniform scale
        val contentWidth = (bounds.lonRange * scale).toFloat()
        val contentHeight = (latRangeMerc * scale).toFloat()

        // Center the content
        val offsetX = (canvasWidth - contentWidth) / 2f
        val offsetY = (canvasHeight - contentHeight) / 2f

        return ViewportTransform(scale, offsetX, offsetY)
    }

    /**
     * Parse coordinates from GeoJSON and create a Path with dynamic bounds
     */
    fun createPath(
        geometry: GeoJsonGeometry,
        width: Float,
        height: Float,
        bounds: Bounds,
        transform: ViewportTransform,
    ): List<Path> {
        return when (geometry.type) {
            "Polygon" -> {
                val coordinates = parsePolygonCoordinates(geometry.coordinates)
                coordinates?.let { listOf(createPolygonPath(it, bounds, transform)) } ?: emptyList()
            }

            "MultiPolygon" -> {
                val polygons = parseMultiPolygonCoordinates(geometry.coordinates)
                polygons.map { polygon ->
                    createPolygonPath(polygon, bounds, transform)
                }
            }

            else -> emptyList()
        }
    }

    private fun createPolygonPath(
        coordinates: List<List<Pair<Double, Double>>>,
        bounds: Bounds,
        transform: ViewportTransform,
    ): Path {
        val path = Path()
        val maxLatMerc = latToMercator(bounds.maxLat)

        coordinates.forEachIndexed { ringIndex, ring ->
            if (ring.isEmpty()) return@forEachIndexed

            val first = ring.first()
            val startX = transform.offsetX + ((first.first - bounds.minLon) * transform.scale).toFloat()
            val startY = transform.offsetY + ((maxLatMerc - latToMercator(first.second)) * transform.scale).toFloat()
            path.moveTo(startX, startY)

            ring.drop(1).forEach { (lon, lat) ->
                val x = transform.offsetX + ((lon - bounds.minLon) * transform.scale).toFloat()
                val y = transform.offsetY + ((maxLatMerc - latToMercator(lat)) * transform.scale).toFloat()
                path.lineTo(x, y)
            }

            path.close()
        }

        return path
    }

    /**
     * Parse Polygon coordinates: [ [[lon, lat], ...], ... ]
     */
    private fun parsePolygonCoordinates(element: JsonElement): List<List<Pair<Double, Double>>>? {
        val array = element as? JsonArray ?: return null
        return array.mapNotNull { ring ->
            parseCoordinateRing(ring)
        }
    }

    /**
     * Parse MultiPolygon coordinates: [ [ [[lon, lat], ...], ... ], ... ]
     */
    private fun parseMultiPolygonCoordinates(element: JsonElement): List<List<List<Pair<Double, Double>>>> {
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { polygonElement ->
            parsePolygonCoordinates(polygonElement)
        }
    }

    /**
     * Parse a ring of coordinates: [[lon, lat], ...]
     */
    private fun parseCoordinateRing(element: JsonElement): List<Pair<Double, Double>>? {
        val array = element as? JsonArray ?: return null
        return array.mapNotNull { coordElement ->
            parseCoordinate(coordElement)
        }
    }

    /**
     * Parse a single coordinate: [lon, lat]
     */
    private fun parseCoordinate(element: JsonElement): Pair<Double, Double>? {
        val array = element as? JsonArray ?: return null
        if (array.size < 2) return null

        val lon = (array[0] as? JsonPrimitive)?.double ?: return null
        val lat = (array[1] as? JsonPrimitive)?.double ?: return null

        return Pair(lon, lat)
    }
}
