package me.matsumo.travelog.core.model.geo

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import me.matsumo.travelog.core.model.geo.OverpassResult.Element.Coordinate

/**
 * Unified geo area model supporting ADM0-5 hierarchy.
 *
 * This replaces the previous GeoRegionGroup (ADM1) and GeoRegion (ADM2) models
 * with a single self-referencing structure.
 *
 * Hierarchy:
 *   ADM0 (Country) -> ADM1 (Prefecture/State) -> ADM2 (City/District) -> ...
 */
@Stable
@Serializable
data class GeoArea(
    @SerialName("id")
    val id: String? = null,

    @SerialName("parent_id")
    val parentId: String? = null,

    @SerialName("level")
    val level: GeoAreaLevel,

    @SerialName("adm_id")
    val admId: String,

    @SerialName("country_code")
    val countryCode: String,

    @SerialName("name")
    val name: String,

    @SerialName("name_en")
    val nameEn: String? = null,

    @SerialName("name_ja")
    val nameJa: String? = null,

    @SerialName("iso_code")
    val isoCode: String? = null,

    @SerialName("wikipedia")
    val wikipedia: String? = null,

    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @SerialName("center")
    val center: Coordinate? = null,

    /**
     * Boundary polygons in MultiPolygon format.
     *
     * This is a List because a single administrative area can have
     * multiple disjoint polygons (e.g., islands, exclaves).
     * Each PolygonWithHoles represents one polygon with optional holes.
     *
     * Note: This is NOT related to children hierarchy.
     * - children: administrative hierarchy (ADM0 -> ADM1 -> ADM2)
     * - polygons: geographic discontinuity (main land + islands)
     */
    @SerialName("polygons")
    val polygons: List<PolygonWithHoles> = emptyList(),

    /**
     * Children areas (lazy loaded, not always populated).
     * For ADM0: contains ADM1 areas
     * For ADM1: contains ADM2 areas
     * etc.
     */
    @SerialName("children")
    val children: List<GeoArea> = emptyList(),
) {
    /**
     * Check if this area is a country (root level).
     */
    val isCountry: Boolean get() = level == GeoAreaLevel.ADM0

    /**
     * Get the child count for display (e.g., "47 regions")
     */
    val childCount: Int get() = children.size

    /**
     * Build GeoJSON MultiPolygon for API payload.
     */
    fun getGeoJsonMultiPolygon() = buildJsonObject {
        put("type", JsonPrimitive("MultiPolygon"))
        put(
            "coordinates",
            JsonArray(
                polygons.map { polygon ->
                    JsonArray(
                        polygon.map { ring ->
                            JsonArray(
                                closeRing(ring).map { coord ->
                                    JsonArray(
                                        listOf(
                                            JsonPrimitive(coord.lon),
                                            JsonPrimitive(coord.lat),
                                        ),
                                    )
                                },
                            )
                        },
                    )
                },
            ),
        )
    }

    /**
     * Build GeoJSON Point for center coordinate.
     */
    fun getGeoJsonPoint() = center?.let { c ->
        buildJsonObject {
            put("type", JsonPrimitive("Point"))
            put(
                "coordinates",
                JsonArray(
                    listOf(
                        JsonPrimitive(c.lon),
                        JsonPrimitive(c.lat),
                    ),
                ),
            )
        }
    }

    /**
     * Get display name with fallback order: name_ja -> name_en -> name
     */
    fun getLocalizedName(preferJapanese: Boolean = true): String {
        return if (preferJapanese) {
            nameJa ?: name
        } else {
            nameEn ?: name
        }
    }

    companion object {
        /**
         * Ensure ring is closed (first == last point).
         */
        fun closeRing(ring: List<Coordinate>): List<Coordinate> {
            if (ring.isEmpty()) return ring
            val first = ring.first()
            val last = ring.last()

            return if (first.lat == last.lat && first.lon == last.lon) {
                ring
            } else {
                ring + first
            }
        }
    }
}
