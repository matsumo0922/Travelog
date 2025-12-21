package me.matsumo.travelog.core.ui.component

import androidx.compose.ui.graphics.Path
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import me.matsumo.travelog.core.model.GeoJsonGeometry
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.tan

/**
 * Helper class for rendering GeoJSON data on Canvas
 */
internal object GeoJsonRenderer {
    /**
     * Convert longitude to X coordinate using Mercator projection
     */
    fun longitudeToX(longitude: Double, width: Float): Float {
        return ((longitude + 180.0) / 360.0 * width).toFloat()
    }

    /**
     * Convert latitude to Y coordinate using Mercator projection
     */
    fun latitudeToY(latitude: Double, height: Float): Float {
        val latRad = latitude * PI / 180.0
        val mercN = ln(tan(PI / 4.0 + latRad / 2.0))
        return (height / 2.0 - (mercN * height / (2.0 * PI))).toFloat()
    }

    /**
     * Parse coordinates from GeoJSON and create a Path
     */
    fun createPath(
        geometry: GeoJsonGeometry,
        width: Float,
        height: Float,
    ): List<Path> {
        return when (geometry.type) {
            "Polygon" -> {
                val coordinates = parsePolygonCoordinates(geometry.coordinates)
                coordinates?.let { listOf(createPolygonPath(it, width, height)) } ?: emptyList()
            }

            "MultiPolygon" -> {
                val polygons = parseMultiPolygonCoordinates(geometry.coordinates)
                polygons.map { polygon ->
                    createPolygonPath(polygon, width, height)
                }
            }

            else -> emptyList()
        }
    }

    private fun createPolygonPath(
        coordinates: List<List<Pair<Double, Double>>>,
        width: Float,
        height: Float,
    ): Path {
        val path = Path()

        coordinates.forEachIndexed { ringIndex, ring ->
            if (ring.isEmpty()) return@forEachIndexed

            val first = ring.first()
            val startX = longitudeToX(first.first, width)
            val startY = latitudeToY(first.second, height)
            path.moveTo(startX, startY)

            ring.drop(1).forEach { (lon, lat) ->
                val x = longitudeToX(lon, width)
                val y = latitudeToY(lat, height)
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
