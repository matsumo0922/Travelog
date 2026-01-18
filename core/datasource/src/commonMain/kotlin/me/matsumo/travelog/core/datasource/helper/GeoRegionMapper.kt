package me.matsumo.travelog.core.datasource.helper

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.matsumo.travelog.core.common.formatter
import me.matsumo.travelog.core.model.dto.GeoRegionDTO
import me.matsumo.travelog.core.model.dto.GeoRegionGroupDTO
import me.matsumo.travelog.core.model.geo.GeoRegion
import me.matsumo.travelog.core.model.geo.GeoRegionGroup
import me.matsumo.travelog.core.model.geo.OverpassResult.Element.Coordinate
import me.matsumo.travelog.core.model.geo.PolygonWithHoles

/**
 * Mapper between domain models (EnrichedAdm1Regions / EnrichedRegion) and DB DTO models
 * (GeoRegionGroupDTO / GeoRegionDTO).
 *
 * Notes:
 * - GeoRegionDTO.centerGeoJson / polygonsGeoJson are expected to be GeoJSON *strings* produced by SQL
 *   (e.g., ST_AsGeoJSON(center::geometry), ST_AsGeoJSON(polygons)).
 * - Domain model polygons are stored as List<PolygonWithHoles>. This mapper can:
 *   - convert Domain -> GeoJSON strings (for RPC payload / client usage),
 *   - convert DTO -> Domain polygons (only if polygonsGeoJson is present and parsable).
 *
 * If you use the "RPC payload without DTO" approach, you may only need:
 * - toGroupDTO(model)
 * - toRegionPayloadJson(model.regions)
 */
class GeoRegionMapper {

    // ---------------------------
    // Domain -> DTO
    // ---------------------------

    fun toGroupDTO(model: GeoRegionGroup, includeGeoJsonString: Boolean = true): GeoRegionGroupDTO {
        val polygonsGeoJson = formatter.encodeToString(model.getGeoJsonMultiPolygon()).takeIf { includeGeoJsonString }

        return GeoRegionGroupDTO(
            id = model.id,
            admId = model.admId,
            admName = model.admName,
            admGroup = model.admGroup,
            admISO = model.admISO,
            name = model.name,
            nameEn = model.nameEn,
            nameJa = model.nameJa,
            thumbnailUrl = model.thumbnailUrl,
            polygonsGeoJson = polygonsGeoJson,
            createdAt = null,
            updatedAt = null,
        )
    }

    /**
     * Convert a single EnrichedRegion to GeoRegionDTO.
     *
     * @param groupId Parent group UUID (geo_region_groups.id)
     * @param includeGeoJsonStrings If true, fills centerGeoJson/polygonsGeoJson by generating GeoJSON strings
     *                             from the domain model. (Useful for client consumption / debugging.)
     *                             If your DTO is used for DB SELECT from view, these will be already set by SQL.
     */
    fun toRegionDTO(
        model: GeoRegion,
        groupId: String,
        includeGeoJsonStrings: Boolean = true,
    ): GeoRegionDTO {
        val centerGeoJson = formatter.encodeToString(model.getGeoJsonPoint()).takeIf { includeGeoJsonStrings }
        val polygonsGeoJson = formatter.encodeToString(model.getGeoJsonMultiPolygon()).takeIf { includeGeoJsonStrings }

        return GeoRegionDTO(
            id = model.id,
            groupId = groupId,
            name = model.name,
            adm2Id = model.adm2Id,
            nameEn = model.nameEn,
            nameJa = model.nameJa,
            wikipedia = model.wikipedia,
            iso31662 = model.iso31662,
            thumbnailUrl = model.thumbnailUrl,
            centerGeoJson = centerGeoJson,
            polygonsGeoJson = polygonsGeoJson,
            createdAt = null,
            updatedAt = null,
        )
    }

    fun toRegionDTOs(
        model: GeoRegionGroup,
        groupId: String,
        includeGeoJsonStrings: Boolean = true,
    ): List<GeoRegionDTO> = model.regions.map { region ->
        toRegionDTO(region, groupId, includeGeoJsonStrings)
    }

    // ---------------------------
    // DTO -> Domain
    // ---------------------------

    fun toDomain(group: GeoRegionGroupDTO, regions: List<GeoRegionDTO>): GeoRegionGroup {
        val groupPolygons = group.polygonsGeoJson?.let { parseGeoJsonMultiPolygon(it) }.orEmpty()

        return toDomainWithoutRegions(group).copy(
            polygons = groupPolygons,
            regions = regions.map { toDomainRegion(it) },
        )
    }

    fun toDomainWithoutRegions(group: GeoRegionGroupDTO): GeoRegionGroup {
        return GeoRegionGroup(
            id = group.id,
            admId = group.admId,
            admName = group.admName,
            admGroup = group.admGroup,
            admISO = group.admISO,
            name = group.name,
            nameEn = group.nameEn,
            nameJa = group.nameJa,
            thumbnailUrl = group.thumbnailUrl,
            polygons = emptyList(),
            regions = emptyList(),
        )
    }

    /**
     * Convert GeoRegionDTO to EnrichedRegion.
     *
     * - centerGeoJson must be a GeoJSON string for Point.
     * - polygonsGeoJson must be a GeoJSON string for MultiPolygon.
     *
     * If centerGeoJson/polygonsGeoJson are null, this will use defaults:
     * - center = (0,0)
     * - polygons = polygonsFallback
     */
    fun toDomainRegion(
        dto: GeoRegionDTO,
        polygonsFallback: List<PolygonWithHoles> = emptyList(),
    ): GeoRegion {
        val center: Coordinate = dto.centerGeoJson?.let { parseGeoJsonPoint(it) }
            ?: Coordinate(lat = 0.0, lon = 0.0)

        val polygons: List<PolygonWithHoles> =
            dto.polygonsGeoJson?.let { parseGeoJsonMultiPolygon(it) } ?: polygonsFallback

        return GeoRegion(
            id = dto.id,
            name = dto.name,
            adm2Id = dto.adm2Id,
            nameEn = dto.nameEn,
            nameJa = dto.nameJa,
            wikipedia = dto.wikipedia,
            iso31662 = dto.iso31662,
            center = center,
            polygons = polygons,
            thumbnailUrl = dto.thumbnailUrl,
        )
    }

    // ---------------------------
    // GeoJSON parsing helpers
    // ---------------------------

    private fun parseGeoJsonPoint(geoJson: String): Coordinate {
        val obj = formatter.parseToJsonElement(geoJson).jsonObject
        val coords = obj["coordinates"] ?: error("Invalid GeoJSON Point: missing coordinates")
        val arr = coords.jsonArray

        val lon = (arr[0] as JsonPrimitive).double
        val lat = (arr[1] as JsonPrimitive).double
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

        val coordinates = obj["coordinates"] ?: error("Invalid GeoJSON MultiPolygon: missing coordinates")
        val polygonsArr = coordinates.jsonArray

        return polygonsArr.map { polygonElem ->
            val ringsArr = polygonElem.jsonArray
            ringsArr.map { ringElem ->
                val ptsArr = ringElem.jsonArray
                ptsArr.map { ptElem ->
                    val pt = ptElem.jsonArray
                    val lon = pt[0].jsonPrimitive.double
                    val lat = pt[1].jsonPrimitive.double

                    Coordinate(lat = lat, lon = lon)
                }
            }
        }
    }
}
