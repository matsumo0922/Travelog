package me.matsumo.travelog.core.model.geo

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EPSILON = 1e-10
private const val EARTH_RADIUS_KM = 6371.0

fun isPointInPolygonWithHoles(point: OverpassResult.Element.Coordinate, polygon: PolygonWithHoles): Boolean {
    if (polygon.isEmpty()) return false

    val outer = polygon.first()
    if (!isPointInRing(point, outer)) return false

    val holes = if (polygon.size > 1) polygon.drop(1) else emptyList()
    if (holes.any { hole -> isPointInRing(point, hole) }) return false

    return true
}

private fun isPointInRing(point: OverpassResult.Element.Coordinate, ring: PolygonRing): Boolean {
    if (ring.size < 3) return false

    var inside = false
    var j = ring.lastIndex

    for (i in ring.indices) {
        val pi = ring[i]
        val pj = ring[j]

        if (isPointOnSegment(point, pj, pi)) return true

        val intersects = ((pi.lat > point.lat) != (pj.lat > point.lat)) &&
                (point.lon <= (pj.lon - pi.lon) * (point.lat - pi.lat) / (pj.lat - pi.lat) + pi.lon)

        if (intersects) inside = !inside

        j = i
    }

    return inside
}

private fun isPointOnSegment(
    point: OverpassResult.Element.Coordinate,
    start: OverpassResult.Element.Coordinate,
    end: OverpassResult.Element.Coordinate,
): Boolean {
    val cross = (point.lat - start.lat) * (end.lon - start.lon) - (point.lon - start.lon) * (end.lat - start.lat)
    if (abs(cross) > EPSILON) return false

    val dot = (point.lon - start.lon) * (point.lon - end.lon) + (point.lat - start.lat) * (point.lat - end.lat)
    return dot <= EPSILON
}

data class BoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
)

fun PolygonWithHoles.boundingBox(): BoundingBox? {
    val allPoints = flatMap { ring -> ring }
    if (allPoints.isEmpty()) return null

    var minLat = Double.POSITIVE_INFINITY
    var maxLat = Double.NEGATIVE_INFINITY
    var minLon = Double.POSITIVE_INFINITY
    var maxLon = Double.NEGATIVE_INFINITY

    allPoints.forEach { coordinate ->
        minLat = minOf(minLat, coordinate.lat)
        maxLat = maxOf(maxLat, coordinate.lat)
        minLon = minOf(minLon, coordinate.lon)
        maxLon = maxOf(maxLon, coordinate.lon)
    }

    return BoundingBox(minLat = minLat, maxLat = maxLat, minLon = minLon, maxLon = maxLon)
}

fun BoundingBox.contains(point: OverpassResult.Element.Coordinate): Boolean {
    return point.lat in minLat..maxLat && point.lon in minLon..maxLon
}

fun BoundingBox.center(): OverpassResult.Element.Coordinate {
    return OverpassResult.Element.Coordinate(
        lat = (minLat + maxLat) / 2,
        lon = (minLon + maxLon) / 2,
    )
}

fun haversineDistanceKm(from: OverpassResult.Element.Coordinate, to: OverpassResult.Element.Coordinate): Double {
    val dLat = (to.lat - from.lat).degToRad()
    val dLon = (to.lon - from.lon).degToRad()

    val lat1 = from.lat.degToRad()
    val lat2 = to.lat.degToRad()

    val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return EARTH_RADIUS_KM * c
}

private fun Double.degToRad(): Double = this / 180.0 * PI
