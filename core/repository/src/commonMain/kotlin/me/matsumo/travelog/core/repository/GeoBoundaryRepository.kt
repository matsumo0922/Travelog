package me.matsumo.travelog.core.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import me.matsumo.travelog.core.datasource.GeoBoundaryDataSource
import me.matsumo.travelog.core.datasource.NominatimDataSource
import me.matsumo.travelog.core.datasource.OverpassDataSource
import me.matsumo.travelog.core.datasource.WikipediaDataSource
import me.matsumo.travelog.core.model.geo.BoundingBox
import me.matsumo.travelog.core.model.geo.EnrichedRegion
import me.matsumo.travelog.core.model.geo.GeoBoundaryLevel
import me.matsumo.travelog.core.model.geo.GeoJsonData
import me.matsumo.travelog.core.model.geo.OverpassResult
import me.matsumo.travelog.core.model.geo.PolygonWithHoles
import me.matsumo.travelog.core.model.geo.boundingBox
import me.matsumo.travelog.core.model.geo.center
import me.matsumo.travelog.core.model.geo.contains
import me.matsumo.travelog.core.model.geo.isPointInPolygonWithHoles
import me.matsumo.travelog.core.model.geo.toIso3CountryCode
import me.matsumo.travelog.core.model.geo.toPolygons

class GeoBoundaryRepository(
    private val geoBoundaryDataSource: GeoBoundaryDataSource,
    private val nominatimDataSource: NominatimDataSource,
    private val overpassDataSource: OverpassDataSource,
    private val wikipediaDataSource: WikipediaDataSource,
) {
    suspend fun getPolygon(countryIso: String, level: GeoBoundaryLevel): GeoJsonData {
        val boundaryInfo = geoBoundaryDataSource.fetchBoundaryInfo(countryIso, level)
        val geoJsonData = geoBoundaryDataSource.downloadGeoJson(boundaryInfo.simplifiedGeometryGeoJSON!!)

        return geoJsonData
    }

    suspend fun getAdmins(location: String): List<OverpassResult.Element> {
        val nominatimResult = nominatimDataSource.search(location)
        val overpassResult = overpassDataSource.getAdmins(nominatimResult.osmId, nominatimResult.placeRank)

        return overpassResult.elements.sortedBy { it.tags.iso31662?.substringAfter("-") ?: "9999" }
    }

    suspend fun getThumbnailUrl(wikipedia: String): String? {
        val lang = wikipedia.substringBefore(':')
        val title = wikipedia.substringAfter(':')

        if (lang.isEmpty() || title.isEmpty()) {
            return null
        }

        return wikipediaDataSource.getThumbnailUrl(lang, title)
    }

    suspend fun getEnrichedAdmins(country: String, query: String?): List<EnrichedRegion> = coroutineScope {
        data class Adm2Region(
            val name: String,
            val polygons: List<PolygonWithHoles>,
            val boundingBoxes: List<BoundingBox>,
            val center: OverpassResult.Element.Coordinate,
        )

        data class Adm1Region(
            val name: String,
            val polygons: List<PolygonWithHoles>,
            val boundingBoxes: List<BoundingBox>,
            val children: MutableList<Adm2Region> = mutableListOf(),
        )

        val iso3CountryCode = country.toIso3CountryCode() ?: error("Unknown county code.")
        val adm1GeoJson = getPolygon(iso3CountryCode, GeoBoundaryLevel.ADM1)
        val adm2GeoJson = getPolygon(iso3CountryCode, GeoBoundaryLevel.ADM2)

        val adm1Regions = adm1GeoJson.features.mapNotNull { feature ->
            val properties = feature.properties as? JsonObject ?: return@mapNotNull null
            val name = properties["shapeName"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val polygons = feature.geometry.toPolygons()
            val boundingBoxes = polygons.mapNotNull { polygon -> polygon.boundingBox() }

            if (polygons.isEmpty() || boundingBoxes.isEmpty()) return@mapNotNull null

            Adm1Region(
                name = name,
                polygons = polygons,
                boundingBoxes = boundingBoxes,
            )
        }

        val adm2Regions = adm2GeoJson.features.mapNotNull { feature ->
            val properties = feature.properties as? JsonObject ?: return@mapNotNull null
            val name = properties["shapeName"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val polygons = feature.geometry.toPolygons()
            val boundingBoxes = polygons.mapNotNull { polygon -> polygon.boundingBox() }
            val center = boundingBoxes.firstOrNull()?.center() ?: return@mapNotNull null

            if (polygons.isEmpty() || boundingBoxes.isEmpty()) return@mapNotNull null

            Adm2Region(
                name = name,
                polygons = polygons,
                boundingBoxes = boundingBoxes,
                center = center,
            )
        }

        adm2Regions.forEach { adm2 ->
            val parent = adm1Regions.firstOrNull { adm1 ->
                adm1.boundingBoxes.any { boundingBox -> boundingBox.contains(adm2.center) } &&
                        adm1.polygons.any { polygon -> isPointInPolygonWithHoles(adm2.center, polygon) }
            }

            parent?.children?.add(adm2)
        }

        Napier.d { "adm1Regions=${adm1Regions.size}, adm2Regions=${adm2Regions.size}" }

        for (adm1 in adm1Regions) {
            Napier.d { "${adm1.name} -> ${adm1.children.size}" }
        }

        val targetAdm1 = adm1Regions.find { adm1 ->
            adm1.name.contains("Saitama", ignoreCase = true) ||
                    (query?.let { adm1.name.contains(it, ignoreCase = true) } == true)
        }

        targetAdm1?.children?.mapIndexed { index, adm2 ->
            EnrichedRegion(
                id = index.toLong(),
                tags = mapOf("name" to adm2.name),
                center = adm2.center,
                polygon = adm2.polygons.firstOrNull() ?: emptyList(),
                thumbnailUrl = null,
            )
        }.orEmpty()
    }
}

