package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import me.matsumo.travelog.core.model.geo.OverpassResult.Element.Coordinate

typealias PolygonRing = List<Coordinate>
typealias PolygonWithHoles = List<PolygonRing>

fun GeoJsonGeometry.toPolygons(): List<PolygonWithHoles> {
    return when (type) {
        "Polygon" -> parsePolygonCoordinates(coordinates)?.let { listOf(it) } ?: emptyList()
        "MultiPolygon" -> parseMultiPolygonCoordinates(coordinates)
        else -> emptyList()
    }
}

fun parsePolygonCoordinates(element: JsonElement): PolygonWithHoles? {
    val array = element as? JsonArray ?: return null
    return array.mapNotNull { ring ->
        parseCoordinateRing(ring)
    }
}

fun parseMultiPolygonCoordinates(element: JsonElement): List<PolygonWithHoles> {
    val array = element as? JsonArray ?: return emptyList()
    return array.mapNotNull { polygonElement ->
        parsePolygonCoordinates(polygonElement)
    }
}

private fun parseCoordinateRing(element: JsonElement): PolygonRing? {
    val array = element as? JsonArray ?: return null
    return array.mapNotNull { coordElement ->
        parseCoordinate(coordElement)
    }
}

private fun parseCoordinate(element: JsonElement): Coordinate? {
    val array = element as? JsonArray ?: return null
    if (array.size < 2) return null

    val lon = (array[0] as? JsonPrimitive)?.double ?: return null
    val lat = (array[1] as? JsonPrimitive)?.double ?: return null

    return Coordinate(lat = lat, lon = lon)
}
