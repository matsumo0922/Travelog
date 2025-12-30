package me.matsumo.travelog.core.model.geo

import kotlin.math.abs

private const val EPSILON = 1e-10

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
