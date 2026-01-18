package me.matsumo.travelog.core.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.datasource.api.GeoRegionApi
import me.matsumo.travelog.core.datasource.helper.GeoRegionMapper
import me.matsumo.travelog.core.model.geo.GeoRegion
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Insert or update a region group with its regions.
     *
     * @param enriched EnrichedAdm1Regions data from GeoBoundaryRepository
     * @return The result of the upsert operation
     */
    suspend fun upsertRegionGroup(enriched: GeoRegionGroup) = withContext(ioDispatcher) {
        geoRegionApi.upsertGroupWithRegions(enriched)
    }

    /**
     * Fetch a region group by ADM1 ID.
     *
     * @param admId The ADM1 identifier
     * @return GeoRegionGroup if found, null otherwise (regions is empty list)
     */
    suspend fun getRegionGroupByAdmId(admId: String): GeoRegionGroup? = withContext(ioDispatcher) {
        val dto = geoRegionApi.fetchGroupByAdmId(admId) ?: return@withContext null
        return@withContext geoRegionMapper.toDomainWithoutRegions(dto)
    }

    /**
     * Fetch all region groups by group code.
     *
     * @param groupCode The group code
     * @return List of GeoRegionGroup (each group's regions is empty list)
     */
    suspend fun getGroupsByGroupCode(groupCode: String): List<GeoRegionGroup> = withContext(ioDispatcher) {
        return@withContext geoRegionApi.fetchGroupsByGroupCode(groupCode).map { dto ->
            geoRegionMapper.toDomainWithoutRegions(dto)
        }
    }

    /**
     * Fetch all regions belonging to a specific group.
     *
     * @param groupId The group identifier (UUID)
     * @return List of GeoRegion
     */
    suspend fun getRegionsByGroupId(groupId: String): List<GeoRegion> = withContext(ioDispatcher) {
        return@withContext geoRegionApi.fetchRegionsByGroupId(groupId).map { dto ->
            geoRegionMapper.toDomainRegion(dto)
        }
    }

    /**
     * Fetch a complete enriched region with group and all its regions.
     *
     * @param admId The ADM1 identifier
     * @return EnrichedAdm1Regions domain model if found, null otherwise
     */
    suspend fun getEnrichedRegionsByAdmId(admId: String): GeoRegionGroup? = withContext(ioDispatcher) {
        val group = geoRegionApi.fetchGroupByAdmId(admId) ?: return@withContext null
        val regions = geoRegionApi.fetchRegionsByGroupId(group.id!!)

        return@withContext geoRegionMapper.toDomain(group, regions)
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
    suspend fun getAvailableCountryCodes(): List<String> = withContext(ioDispatcher) {
        return@withContext geoRegionApi.fetchDistinctAdmGroups()
    }
}
