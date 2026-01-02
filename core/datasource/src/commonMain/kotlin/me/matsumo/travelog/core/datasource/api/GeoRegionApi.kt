package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import me.matsumo.travelog.core.model.dto.GeoRegionDTO
import me.matsumo.travelog.core.model.dto.GeoRegionGroupDTO
import me.matsumo.travelog.core.model.geo.EnrichedAdm1Regions

class GeoRegionApi internal constructor(
    private val supabaseClient: SupabaseClient,
) {
    suspend fun upsertGroupWithRegions(enriched: EnrichedAdm1Regions): JsonElement {
        val regionsPayload = buildRegionsPayload(enriched)
        val groupPolygons = enriched.getGeoJsonMultiPolygon()

        val raw = supabaseClient.postgrest.rpc(
            function = "upsert_geo_region_group_with_regions",
            parameters = buildJsonObject {
                put("p_adm_id", JsonPrimitive(enriched.admId))
                put("p_adm_name", JsonPrimitive(enriched.admName))
                put("p_adm_group", JsonPrimitive(enriched.admGroup))
                put("p_adm_iso", JsonPrimitive(enriched.admISO))
                put("p_group_polygons_geojson", groupPolygons)
                put("p_regions", regionsPayload)
            }
        ).decodeAs<JsonElement>()

        return raw
    }

    suspend fun fetchGroupByAdmId(admId: String): GeoRegionGroupDTO? =
        supabaseClient.from(GROUP_TABLE_NAME)
            .select {
                filter { eq("adm_id", admId) }
                limit(1)
            }
            .decodeSingleOrNull()

    suspend fun fetchRegionsByGroupId(groupId: String): List<GeoRegionDTO> =
        supabaseClient.from(REGION_VIEW_NAME) // "geo_regions_view"
            .select {
                filter { eq("group_id", groupId) }
                order("name", Order.ASCENDING)
            }
            .decodeList()


    private fun buildRegionsPayload(enriched: EnrichedAdm1Regions): JsonArray =
        JsonArray(
            enriched.regions.map { region ->
                buildJsonObject {
                    put("adm2_id", JsonPrimitive(region.adm2Id))
                    put("name", JsonPrimitive(region.name))

                    region.nameEn?.let { put("name_en", JsonPrimitive(it)) }
                    region.nameJa?.let { put("name_ja", JsonPrimitive(it)) }
                    region.wikipedia?.let { put("wikipedia", JsonPrimitive(it)) }
                    region.iso31662?.let { put("iso3166_2", JsonPrimitive(it)) }
                    region.thumbnailUrl?.let { put("thumbnail_url", JsonPrimitive(it)) }

                    put("center_lat", JsonPrimitive(region.center.lat))
                    put("center_lon", JsonPrimitive(region.center.lon))

                    // Kotlin side: build GeoJSON MultiPolygon JsonObject
                    put("polygons_geojson", region.getGeoJsonMultiPolygon())
                }
            }
        )

    companion object {
        private const val GROUP_TABLE_NAME = "geo_region_groups"
        private const val REGION_VIEW_NAME = "geo_regions_view"
    }
}