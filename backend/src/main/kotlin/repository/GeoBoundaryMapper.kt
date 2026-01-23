package repository

import io.github.aakira.napier.Napier
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import me.matsumo.travelog.core.model.geo.BoundingBox
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.model.geo.GeoAreaLevel
import me.matsumo.travelog.core.model.geo.GeoJsonData
import me.matsumo.travelog.core.model.geo.OverpassResult
import me.matsumo.travelog.core.model.geo.PolygonWithHoles
import me.matsumo.travelog.core.model.geo.boundingBox
import me.matsumo.travelog.core.model.geo.center
import me.matsumo.travelog.core.model.geo.contains
import me.matsumo.travelog.core.model.geo.isPointInPolygonWithHoles
import me.matsumo.travelog.core.model.geo.toPolygons

class GeoBoundaryMapper {

    data class Adm2Region(
        val id: String,
        val name: String,
        val polygons: List<PolygonWithHoles>,
        val boundingBoxes: List<BoundingBox>,
        val center: OverpassResult.Element.Coordinate,
    )

    data class Adm1Region(
        val id: String,
        val name: String,
        val group: String,
        val iso: String,
        val polygons: List<PolygonWithHoles>,
        val boundingBoxes: List<BoundingBox>,
        val children: MutableList<Adm2Region> = mutableListOf(),
    )

    fun mapAdm1Regions(geoJsonData: GeoJsonData): List<Adm1Region> {
        return geoJsonData.features.mapNotNull { feature ->
            val properties = feature.properties as? JsonObject ?: return@mapNotNull null
            val id = properties["shapeID"]?.jsonPrimitive?.contentOrNull
            val name = properties["shapeName"]?.jsonPrimitive?.contentOrNull
            val group = properties["shapeGroup"]?.jsonPrimitive?.contentOrNull
            val iso = properties["shapeISO"]?.jsonPrimitive?.contentOrNull
            val polygons = feature.geometry.toPolygons()
            val boundingBoxes = polygons.mapNotNull { polygon -> polygon.boundingBox() }

            if (id == null || name == null || group == null || iso == null) {
                Napier.d { "Skipping Adm1Region due to missing properties: id=$id, name=$name, group=$group, iso=$iso" }
                return@mapNotNull null
            }

            if (polygons.isEmpty() || boundingBoxes.isEmpty()) return@mapNotNull null

            Adm1Region(
                id = id,
                name = name,
                group = group,
                iso = iso,
                polygons = polygons,
                boundingBoxes = boundingBoxes,
            )
        }
    }

    fun mapAdm2Regions(geoJsonData: GeoJsonData): List<Adm2Region> {
        return geoJsonData.features.mapNotNull { feature ->
            val properties = feature.properties as? JsonObject ?: return@mapNotNull null
            val shapeId = properties["shapeID"]?.jsonPrimitive?.contentOrNull ?: "unknown"
            val name = properties["shapeName"]?.jsonPrimitive?.contentOrNull
                ?: properties["shapeISO"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: shapeId
            val polygons = feature.geometry.toPolygons()
            val boundingBoxes = polygons.mapNotNull { polygon -> polygon.boundingBox() }
            val center = polygons.asSequence()
                .mapNotNull { polygon -> findInteriorPoint(polygon) }
                .firstOrNull()
                ?: return@mapNotNull null

            if (polygons.isEmpty() || boundingBoxes.isEmpty()) return@mapNotNull null

            Adm2Region(
                id = shapeId,
                name = name,
                polygons = polygons,
                boundingBoxes = boundingBoxes,
                center = center,
            )
        }
    }

    fun linkAdm2ToAdm1(
        adm1Regions: List<Adm1Region>,
        adm2Regions: List<Adm2Region>,
    ) {
        adm2Regions.forEach { adm2 ->
            val parent = adm1Regions.firstOrNull { adm1 ->
                adm1.boundingBoxes.any { boundingBox -> boundingBox.contains(adm2.center) } &&
                        adm1.polygons.any { polygon -> isPointInPolygonWithHoles(adm2.center, polygon) }
            }

            parent?.children?.add(adm2)
        }
    }

    fun matchAdm2WithOverpass(
        adm2Regions: List<Adm2Region>,
        overpassElements: List<OverpassResult.Element>,
    ): Map<String, OverpassResult.Element> {
        if (adm2Regions.isEmpty() || overpassElements.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, OverpassResult.Element>()

        overpassElements.forEach { element ->
            val matchedAdm2 = adm2Regions.firstOrNull { adm2 ->
                val center = element.center ?: return@firstOrNull false
                val bbox = adm2.boundingBoxes.any { bbox -> bbox.contains(center) }
                val polygons = adm2.polygons.any { polygon -> isPointInPolygonWithHoles(center, polygon) }

                bbox && polygons
            }

            if (matchedAdm2 != null && matchedAdm2.id !in result) {
                result[matchedAdm2.id] = element
            }
        }

        return result
    }

    fun findTargetAdm1(
        adm1Regions: List<Adm1Region>,
        query: String?,
    ): Adm1Region? {
        return adm1Regions.find { adm1 ->
            adm1.name.contains("Saitama", ignoreCase = true) ||
                    (query?.let { adm1.name.contains(it, ignoreCase = true) } == true)
        }
    }

    private fun polygonCentroid(polygon: PolygonWithHoles): OverpassResult.Element.Coordinate? {
        val outer = polygon.firstOrNull() ?: return null
        if (outer.size < 3) return null

        var accumulatedCross = 0.0
        var accumulatedCx = 0.0
        var accumulatedCy = 0.0

        for (i in outer.indices) {
            val current = outer[i]
            val next = outer[(i + 1) % outer.size]

            val cross = current.lon * next.lat - next.lon * current.lat
            accumulatedCross += cross
            accumulatedCx += (current.lon + next.lon) * cross
            accumulatedCy += (current.lat + next.lat) * cross
        }

        if (accumulatedCross == 0.0) return null

        val areaFactor = accumulatedCross * 3

        return OverpassResult.Element.Coordinate(
            lat = accumulatedCy / areaFactor,
            lon = accumulatedCx / areaFactor,
        )
    }

    private fun findInteriorPoint(polygon: PolygonWithHoles): OverpassResult.Element.Coordinate? {
        val centroid = polygonCentroid(polygon)
        if (centroid != null && isPointInPolygonWithHoles(centroid, polygon)) {
            return centroid
        }

        val bboxCenter = polygon.boundingBox()?.center()
        if (bboxCenter != null && isPointInPolygonWithHoles(bboxCenter, polygon)) {
            return bboxCenter
        }

        return polygon.firstOrNull()?.firstOrNull()
    }

    // ---------------------------
    // GeoArea Conversion
    // ---------------------------

    /**
     * Convert Adm1Region to GeoArea (ADM1 level).
     */
    fun toGeoArea(
        adm1: Adm1Region,
        countryCode: String,
        enrichedData: EnrichedAdm1Data? = null,
        children: List<GeoArea> = emptyList(),
    ): GeoArea {
        val center = adm1.polygons.asSequence()
            .mapNotNull { polygon -> findInteriorPoint(polygon) }
            .firstOrNull()

        return GeoArea(
            id = null,
            parentId = null,
            level = GeoAreaLevel.ADM1,
            admId = adm1.id,
            countryCode = countryCode,
            name = enrichedData?.name ?: adm1.name,
            nameEn = enrichedData?.nameEn,
            nameJa = enrichedData?.nameJa,
            isoCode = adm1.iso,
            wikipedia = enrichedData?.wikipedia,
            thumbnailUrl = enrichedData?.thumbnailUrl,
            center = center,
            polygons = adm1.polygons,
            children = children,
        )
    }

    /**
     * Convert Adm2Region to GeoArea (ADM2 level).
     */
    fun toGeoArea(
        adm2: Adm2Region,
        countryCode: String,
        enrichedData: EnrichedAdm2Data? = null,
    ): GeoArea {
        return GeoArea(
            id = null,
            parentId = null,
            level = GeoAreaLevel.ADM2,
            admId = adm2.id,
            countryCode = countryCode,
            name = enrichedData?.name ?: adm2.name,
            nameEn = enrichedData?.nameEn,
            nameJa = enrichedData?.nameJa,
            isoCode = enrichedData?.isoCode,
            wikipedia = enrichedData?.wikipedia,
            thumbnailUrl = enrichedData?.thumbnailUrl,
            center = adm2.center,
            polygons = adm2.polygons,
            children = emptyList(),
        )
    }

    /**
     * Create ADM0 (country) level GeoArea.
     */
    fun createCountryArea(
        countryCode: String,
        name: String,
        nameEn: String?,
        nameJa: String?,
        polygons: List<PolygonWithHoles>,
        wikipedia: String? = null,
        thumbnailUrl: String? = null,
    ): GeoArea {
        val center = polygons.asSequence()
            .mapNotNull { polygon -> findInteriorPoint(polygon) }
            .firstOrNull()

        return GeoArea(
            id = null,
            parentId = null,
            level = GeoAreaLevel.ADM0,
            admId = countryCode,
            countryCode = countryCode,
            name = name,
            nameEn = nameEn,
            nameJa = nameJa,
            isoCode = countryCode,
            wikipedia = wikipedia,
            thumbnailUrl = thumbnailUrl,
            center = center,
            polygons = polygons,
            children = emptyList(),
        )
    }

    /**
     * Enriched data for ADM1 region from Overpass/Wikipedia.
     */
    data class EnrichedAdm1Data(
        val name: String,
        val nameEn: String?,
        val nameJa: String?,
        val wikipedia: String?,
        val thumbnailUrl: String?,
    )

    /**
     * Enriched data for ADM2 region from Overpass/Wikipedia.
     */
    data class EnrichedAdm2Data(
        val name: String,
        val nameEn: String?,
        val nameJa: String?,
        val isoCode: String?,
        val wikipedia: String?,
        val thumbnailUrl: String?,
    )
}
