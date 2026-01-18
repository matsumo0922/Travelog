package me.matsumo.travelog.core.repository

import me.matsumo.travelog.core.datasource.api.GeoRegionApi
import me.matsumo.travelog.core.datasource.helper.GeoRegionMapper
import me.matsumo.travelog.core.model.dto.GeoRegionDTO
import me.matsumo.travelog.core.model.dto.GeoRegionGroupDTO
import me.matsumo.travelog.core.model.geo.GeoRegionGroup

/**
 * Repository for managing geo regions and region groups in the database.
 *
 * This repository provides higher-level operations for storing and retrieving
 * enriched region data from GeoBoundaryRepository into Supabase.
 */
class GeoRegionRepository(
    private val geoRegionApi: GeoRegionApi,
    private val geoRegionMapper: GeoRegionMapper,
) {
    /**
     * Insert or update a region group with its regions.
     *
     * @param enriched EnrichedAdm1Regions data from GeoBoundaryRepository
     * @return The result of the upsert operation
     */
    suspend fun upsertRegionGroup(enriched: GeoRegionGroup) {
        geoRegionApi.upsertGroupWithRegions(enriched)
    }

    /**
     * Fetch a region group by ADM1 ID.
     *
     * @param admId The ADM1 identifier
     * @return GeoRegionGroupDTO if found, null otherwise
     */
    suspend fun getRegionGroupByAdmId(admId: String): GeoRegionGroupDTO? {
        return geoRegionApi.fetchGroupByAdmId(admId)
    }

    /**
     * Fetch all region groups by group code.
     *
     * @param groupCode The group code
     * @return List of GeoRegionGroupDTO
     */
    suspend fun getGroupsByGroupCode(groupCode: String): List<GeoRegionGroupDTO> {
        return geoRegionApi.fetchGroupsByGroupCode(groupCode)
    }

    /**
     * Fetch all regions belonging to a specific group.
     *
     * @param groupId The group identifier (UUID)
     * @return List of GeoRegionDTO
     */
    suspend fun getRegionsByGroupId(groupId: String): List<GeoRegionDTO> {
        return geoRegionApi.fetchRegionsByGroupId(groupId)
    }

    /**
     * Fetch a complete enriched region with group and all its regions.
     *
     * @param admId The ADM1 identifier
     * @return EnrichedAdm1Regions domain model if found, null otherwise
     */
    suspend fun getEnrichedRegionsByAdmId(admId: String): GeoRegionGroup? {
        val group = geoRegionApi.fetchGroupByAdmId(admId) ?: return null
        val regions = geoRegionApi.fetchRegionsByGroupId(group.id!!)

        return geoRegionMapper.toDomain(group, regions)
    }

    /**
     * Batch upsert multiple region groups.
     *
     * @param enrichedList List of EnrichedAdm1Regions to insert/update
     */
    suspend fun upsertRegionGroups(enrichedList: List<GeoRegionGroup>) {
        enrichedList.forEach { enriched ->
            upsertRegionGroup(enriched)
        }
    }

    /**
     * Fetch multiple enriched regions by ADM1 IDs.
     *
     * @param admIds List of ADM1 identifiers
     * @return List of EnrichedAdm1Regions (excludes not found items)
     */
    suspend fun getEnrichedRegionsByAdmIds(admIds: List<String>): List<GeoRegionGroup> {
        return admIds.mapNotNull { admId ->
            getEnrichedRegionsByAdmId(admId)
        }
    }

    /**
     * Fetch all available country codes (adm_group) from the database.
     *
     * @return List of unique country codes
     */
    suspend fun getAvailableCountryCodes(): List<String> {
        return geoRegionApi.fetchDistinctAdmGroups()
    }
}
