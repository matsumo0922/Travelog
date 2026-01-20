package me.matsumo.travelog.core.datasource.helper

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.matsumo.travelog.core.common.formatter
import me.matsumo.travelog.core.model.dto.GeoAreaDTO
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.model.geo.GeoAreaLevel
import me.matsumo.travelog.core.model.geo.OverpassResult.Element.Coordinate
import me.matsumo.travelog.core.model.geo.PolygonWithHoles

/**
 * Mapper between GeoArea domain model and GeoAreaDTO.
 */
class GeoAreaMapper {

    // ---------------------------
    // Domain -> DTO
    // ---------------------------

    fun toDTO(model: GeoArea, includeGeoJson: Boolean = true): GeoAreaDTO {
        val centerGeoJson = if (includeGeoJson) {
            model.getGeoJsonPoint()?.let { formatter.encodeToString(it) }
        } else null

        val polygonsGeoJson = if (includeGeoJson && model.polygons.isNotEmpty()) {
            formatter.encodeToString(model.getGeoJsonMultiPolygon())
        } else null

        return GeoAreaDTO(
            id = model.id,
            parentId = model.parentId,
            level = model.level.value,
            admId = model.admId,
            countryCode = model.countryCode,
            name = model.name,
            nameEn = model.nameEn,
            nameJa = model.nameJa,
            isoCode = model.isoCode,
            wikipedia = model.wikipedia,
            thumbnailUrl = model.thumbnailUrl,
            centerGeoJson = centerGeoJson,
            polygonsGeoJson = polygonsGeoJson,
        )
    }

    // ---------------------------
    // DTO -> Domain
    // ---------------------------

    fun toDomain(dto: GeoAreaDTO): GeoArea {
        val center = dto.centerGeoJson?.let { parseGeoJsonPoint(it) }
        val polygons = dto.polygonsGeoJson?.let { parseGeoJsonMultiPolygon(it) } ?: emptyList()

        return GeoArea(
            id = dto.id,
            parentId = dto.parentId,
            level = GeoAreaLevel.fromInt(dto.level),
            admId = dto.admId,
            countryCode = dto.countryCode,
            name = dto.name,
            nameEn = dto.nameEn,
            nameJa = dto.nameJa,
            isoCode = dto.isoCode,
            wikipedia = dto.wikipedia,
            thumbnailUrl = dto.thumbnailUrl,
            center = center,
            polygons = polygons,
            children = emptyList(),
        )
    }

    /**
     * Convert flat list of DTOs to hierarchical domain models.
     * Groups by parent_id and builds tree structure.
     */
    fun toDomainHierarchy(dtos: List<GeoAreaDTO>): List<GeoArea> {
        val areaMap = dtos.associateBy { it.id }.mapValues { (_, dto) -> toDomain(dto) }.toMutableMap()
        val childrenMap = dtos.groupBy { it.parentId }

        // Build hierarchy starting from roots (null parent)
        return buildHierarchy(null, areaMap, childrenMap)
    }

    private fun buildHierarchy(
        parentId: String?,
        areaMap: Map<String?, GeoArea>,
        childrenMap: Map<String?, List<GeoAreaDTO>>,
    ): List<GeoArea> {
        val children = childrenMap[parentId] ?: return emptyList()

        return children.mapNotNull { dto ->
            val area = areaMap[dto.id] ?: return@mapNotNull null
            val subChildren = buildHierarchy(dto.id, areaMap, childrenMap)
            area.copy(children = subChildren)
        }
    }

    // ---------------------------
    // GeoJSON parsing helpers
    // ---------------------------

    private fun parseGeoJsonPoint(geoJson: String): Coordinate {
        val obj = formatter.parseToJsonElement(geoJson).jsonObject
        val coords = obj["coordinates"]?.jsonArray ?: error("Invalid GeoJSON Point: missing coordinates")

        val lon = (coords[0] as JsonPrimitive).double
        val lat = (coords[1] as JsonPrimitive).double
        return Coordinate(lat = lat, lon = lon)
    }

    /**
     * Parse GeoJSON MultiPolygon into List<PolygonWithHoles>.
     *
     * MultiPolygon.coordinates:
     * [
     *   [ // polygon 1
     *     [ [lon,lat], ... ], // outer ring
     *     [ [lon,lat], ... ]  // hole ring (optional)
     *   ],
     *   ...
     * ]
     */
    private fun parseGeoJsonMultiPolygon(geoJson: String): List<PolygonWithHoles> {
        val obj = formatter.parseToJsonElement(geoJson).jsonObject
        val type = obj["type"]?.jsonPrimitive?.content

        require(type == "MultiPolygon") { "Expected GeoJSON type MultiPolygon but was $type" }

        val coordinates = obj["coordinates"]?.jsonArray ?: error("Invalid GeoJSON MultiPolygon: missing coordinates")

        return coordinates.map { polygonElem ->
            polygonElem.jsonArray.map { ringElem ->
                ringElem.jsonArray.map { ptElem ->
                    val pt = ptElem.jsonArray
                    Coordinate(
                        lat = pt[1].jsonPrimitive.double,
                        lon = pt[0].jsonPrimitive.double,
                    )
                }
            }
        }
    }
}
