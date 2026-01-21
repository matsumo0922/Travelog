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
     * Fetch a single area by adm_id.
     */
    suspend fun fetchAreaByAdmId(admId: String): GeoAreaDTO? {
        return supabaseClient.from(VIEW_NAME)
            .select {
                filter {
                    eq("adm_id", admId)
                }
                limit(1)
            }
            .decodeSingleOrNull()
    }

    /**
     * Fetch areas with missing name_en or name_ja.
     * @param countryCode The country code to filter by
     * @param level The administrative level to filter by (null for all levels)
     * @param missingNameEn If true, returns areas where name_en is null
     * @param missingNameJa If true, returns areas where name_ja is null
     */
    suspend fun fetchAreasWithMissingNames(
        countryCode: String,
        level: Int? = null,
        missingNameEn: Boolean = true,
        missingNameJa: Boolean = true,
    ): List<GeoAreaDTO> {
        return supabaseClient.from(VIEW_NAME)
            .select {
                filter {
                    eq("country_code", countryCode)
                    level?.let { eq("level", it) }
                    if (missingNameEn && missingNameJa) {
                        or {
                            exact("name_en", null)
                            exact("name_ja", null)
                        }
                    } else if (missingNameEn) {
                        exact("name_en", null)
                    } else if (missingNameJa) {
                        exact("name_ja", null)
                    }
                }
            }
            .decodeList()
    }

    /**
     * Update name_en and name_ja for a specific area.
     * @param areaId The UUID of the area to update
     * @param nameEn The new English name (null to keep existing)
     * @param nameJa The new Japanese name (null to keep existing)
     */
    suspend fun updateAreaNames(
        areaId: String,
        nameEn: String?,
        nameJa: String?,
    ) {
        supabaseClient.from(TABLE_NAME)
            .update(
                buildJsonObject {
                    nameEn?.let { put("name_en", JsonPrimitive(it)) }
                    nameJa?.let { put("name_ja", JsonPrimitive(it)) }
                },
            ) {
                filter { eq("id", areaId) }
            }
    }

    /**
     * Batch update names for multiple areas.
     * @param updates List of pairs (areaId, nameEn, nameJa)
     */
    suspend fun updateAreaNamesBatch(updates: List<NameUpdateItem>) {
        val payload = JsonArray(
            updates.map { item ->
                buildJsonObject {
                    put("id", JsonPrimitive(item.areaId))
                    item.nameEn?.let { put("name_en", JsonPrimitive(it)) }
                    item.nameJa?.let { put("name_ja", JsonPrimitive(it)) }
                }
            },
        )

        supabaseClient.postgrest.rpc(
            function = "update_geo_area_names_batch",
            parameters = buildJsonObject {
                put("p_updates", payload)
            },
        )
    }

    /**
     * Get count of areas with missing names by country (optionally filtered by level).
     */
    suspend fun getMissingNamesCount(countryCode: String, level: Int? = null): MissingNamesCount {
        val result = supabaseClient.postgrest.rpc(
            function = "get_missing_names_count",
            parameters = buildJsonObject {
                put("p_country_code", JsonPrimitive(countryCode))
                level?.let { put("p_level", JsonPrimitive(it)) }
            },
        ).decodeAs<JsonObject>()

        return MissingNamesCount(
            total = result["total"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            missingNameEn = result["missing_name_en"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            missingNameJa = result["missing_name_ja"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
        )
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

/**
 * Data class for batch name update
 */
data class NameUpdateItem(
    val areaId: String,
    val nameEn: String?,
    val nameJa: String?,
)

/**
 * Data class for missing names count result
 */
data class MissingNamesCount(
    val total: Int,
    val missingNameEn: Int,
    val missingNameJa: Int,
)
