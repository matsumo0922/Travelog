package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.matsumo.travelog.core.model.dto.GeoAreaDTO
import me.matsumo.travelog.core.model.geo.GeoArea

class GeoAreaApi internal constructor(
    private val supabaseClient: SupabaseClient,
) {
    /**
     * Upsert a single geo area.
     * Returns the area UUID.
     */
    suspend fun upsertArea(area: GeoArea): String {
        val result = supabaseClient.postgrest.rpc(
            function = "upsert_geo_area",
            parameters = buildAreaParams(area),
        ).decodeAs<String>()

        return result.removeSurrounding("\"")
    }

    /**
     * Batch upsert geo areas.
     * Returns map of adm_id -> UUID.
     */
    suspend fun upsertAreasBatch(areas: List<GeoArea>): Map<String, String> {
        val payload = JsonArray(areas.map { buildBatchItemParams(it) })

        val result = supabaseClient.postgrest.rpc(
            function = "upsert_geo_areas_batch",
            parameters = buildJsonObject {
                put("p_areas", payload)
            },
        ).decodeAs<JsonArray>()

        return result.associate { item ->
            val obj = item as JsonObject
            val admId = obj["adm_id"]?.jsonPrimitive?.content ?: ""
            val id = obj["id"]?.jsonPrimitive?.content ?: ""
            admId to id
        }
    }

    /**
     * Fetch all areas for a country, optionally limited by level.
     */
    suspend fun fetchAreasByCountry(countryCode: String, maxLevel: Int? = null): List<GeoAreaDTO> {
        return if (maxLevel != null) {
            supabaseClient.postgrest.rpc(
                function = "get_geo_areas_by_country",
                parameters = buildJsonObject {
                    put("p_country_code", JsonPrimitive(countryCode))
                    put("p_max_level", JsonPrimitive(maxLevel))
                },
            ).decodeList()
        } else {
            supabaseClient.postgrest.rpc(
                function = "get_geo_areas_by_country",
                parameters = buildJsonObject {
                    put("p_country_code", JsonPrimitive(countryCode))
                },
            ).decodeList()
        }
    }

    /**
     * Fetch direct children of an area.
     */
    suspend fun fetchChildren(parentId: String): List<GeoAreaDTO> =
        supabaseClient.postgrest.rpc(
            function = "get_geo_area_children",
            parameters = buildJsonObject {
                put("p_parent_id", JsonPrimitive(parentId))
            },
        ).decodeList()

    /**
     * Fetch all descendants of an area recursively.
     */
    suspend fun fetchDescendants(areaId: String, maxDepth: Int = 10): List<GeoAreaDTO> =
        supabaseClient.postgrest.rpc(
            function = "get_geo_area_descendants",
            parameters = buildJsonObject {
                put("p_area_id", JsonPrimitive(areaId))
                put("p_max_depth", JsonPrimitive(maxDepth))
            },
        ).decodeList()

    /**
     * Fetch ancestors of an area (path to root).
     */
    suspend fun fetchAncestors(areaId: String): List<GeoAreaDTO> =
        supabaseClient.postgrest.rpc(
            function = "get_geo_area_ancestors",
            parameters = buildJsonObject {
                put("p_area_id", JsonPrimitive(areaId))
            },
        ).decodeList()

    /**
     * Fetch areas at a specific level for a country.
     */
    suspend fun fetchAreasByLevel(countryCode: String, level: Int): List<GeoAreaDTO> =
        supabaseClient.postgrest.rpc(
            function = "get_geo_areas_by_level",
            parameters = buildJsonObject {
                put("p_country_code", JsonPrimitive(countryCode))
                put("p_level", JsonPrimitive(level))
            },
        ).decodeList()

    /**
     * Fetch all available country codes.
     */
    suspend fun fetchDistinctCountryCodes(): List<String> =
        supabaseClient.postgrest.rpc(
            function = "get_distinct_country_codes",
        ).decodeList<JsonObject>()
            .mapNotNull { it["country_code"]?.jsonPrimitive?.content }

    /**
     * Fetch a single area by ID.
     */
    suspend fun fetchAreaById(areaId: String): GeoAreaDTO? =
        supabaseClient.from(VIEW_NAME)
            .select {
                filter { eq("id", areaId) }
                limit(1)
            }
            .decodeSingleOrNull()

    /**
     * Fetch a single area by adm_id and parent_id.
     * For root level areas (parentId = null), queries for level = 0.
     */
    suspend fun fetchAreaByAdmId(admId: String, parentId: String?): GeoAreaDTO? {
        return if (parentId != null) {
            supabaseClient.from(VIEW_NAME)
                .select {
                    filter {
                        eq("adm_id", admId)
                        eq("parent_id", parentId)
                    }
                    limit(1)
                }
                .decodeSingleOrNull()
        } else {
            // For root level, query by adm_id and level = 0
            supabaseClient.from(VIEW_NAME)
                .select {
                    filter {
                        eq("adm_id", admId)
                        eq("level", 0)
                    }
                    limit(1)
                }
                .decodeSingleOrNull()
        }
    }

    /**
     * Build params for single upsert RPC (requires p_ prefix for PostgREST function matching)
     */
    private fun buildAreaParams(area: GeoArea): JsonObject = buildJsonObject {
        put("p_parent_id", area.parentId?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_level", JsonPrimitive(area.level.value))
        put("p_adm_id", JsonPrimitive(area.admId))
        put("p_country_code", JsonPrimitive(area.countryCode))
        put("p_name", JsonPrimitive(area.name))
        put("p_name_en", area.nameEn?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_name_ja", area.nameJa?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_iso_code", area.isoCode?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_wikipedia", area.wikipedia?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_thumbnail_url", area.thumbnailUrl?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_center_lat", area.center?.let { JsonPrimitive(it.lat) } ?: JsonNull)
        put("p_center_lon", area.center?.let { JsonPrimitive(it.lon) } ?: JsonNull)
        put("p_polygons_geojson", if (area.polygons.isNotEmpty()) area.getGeoJsonMultiPolygon() else JsonNull)
    }

    /**
     * Build params for batch upsert JSONB array items (no p_ prefix, keys match SQL function's JSON extraction)
     */
    private fun buildBatchItemParams(area: GeoArea): JsonObject = buildJsonObject {
        area.parentId?.let { put("parent_id", JsonPrimitive(it)) }
        put("level", JsonPrimitive(area.level.value))
        put("adm_id", JsonPrimitive(area.admId))
        put("country_code", JsonPrimitive(area.countryCode))
        put("name", JsonPrimitive(area.name))
        area.nameEn?.let { put("name_en", JsonPrimitive(it)) }
        area.nameJa?.let { put("name_ja", JsonPrimitive(it)) }
        area.isoCode?.let { put("iso_code", JsonPrimitive(it)) }
        area.wikipedia?.let { put("wikipedia", JsonPrimitive(it)) }
        area.thumbnailUrl?.let { put("thumbnail_url", JsonPrimitive(it)) }
        area.center?.let {
            put("center_lat", JsonPrimitive(it.lat))
            put("center_lon", JsonPrimitive(it.lon))
        }
        if (area.polygons.isNotEmpty()) {
            put("polygons_geojson", area.getGeoJsonMultiPolygon())
        }
    }

    companion object {
        private const val TABLE_NAME = "geo_areas"
        private const val VIEW_NAME = "geo_areas_view"
    }
}
