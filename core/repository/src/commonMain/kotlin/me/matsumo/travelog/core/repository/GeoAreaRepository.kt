package me.matsumo.travelog.core.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.datasource.GeoAreaCacheDataSource
import me.matsumo.travelog.core.datasource.api.GeoAreaApi
import me.matsumo.travelog.core.datasource.helper.GeoAreaMapper
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.model.geo.GeoAreaLevel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Repository for managing geo areas in the database.
 *
 * This repository provides operations for storing and retrieving hierarchical
 * geo area data (ADM0 -> ADM1 -> ADM2).
 */
class GeoAreaRepository(
    private val geoAreaApi: GeoAreaApi,
    private val geoAreaMapper: GeoAreaMapper,
    private val geoAreaCacheDataSource: GeoAreaCacheDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Upsert a single geo area.
     * Returns the area UUID.
     */
    suspend fun upsertArea(area: GeoArea): String = withContext(ioDispatcher) {
        geoAreaApi.upsertArea(area)
    }

    /**
     * Batch upsert geo areas.
     * Areas should be sorted by level (parent before children) for proper insertion.
     * Returns map of adm_id -> UUID.
     */
    suspend fun upsertAreasBatch(areas: List<GeoArea>): Map<String, String> = withContext(ioDispatcher) {
        geoAreaApi.upsertAreasBatch(areas)
    }

    /**
     * Upsert a country with all its ADM1 and ADM2 regions.
     * Handles the proper ordering: ADM0 -> ADM1 -> ADM2.
     *
     * @param country The country (ADM0) area
     * @param adm1Areas List of ADM1 areas (will have parentId set to countryId)
     * @param adm2AreasByParent Map of ADM1 admId -> List of ADM2 areas
     * @return The country UUID
     */
    suspend fun upsertCountryHierarchy(
        country: GeoArea,
        adm1Areas: List<GeoArea>,
        adm2AreasByParent: Map<String, List<GeoArea>>,
    ): String = withContext(ioDispatcher) {
        // 1. Upsert country (ADM0)
        val countryId = geoAreaApi.upsertArea(country)

        // 2. Upsert ADM1 areas with country as parent
        val adm1WithParent = adm1Areas.map { it.copy(parentId = countryId) }
        val adm1IdMap = geoAreaApi.upsertAreasBatch(adm1WithParent)

        // 3. Upsert ADM2 areas with respective ADM1 as parent
        adm2AreasByParent.forEach { (adm1AdmId, adm2List) ->
            val adm1Id = adm1IdMap[adm1AdmId] ?: return@forEach
            val adm2WithParent = adm2List.map { it.copy(parentId = adm1Id) }
            geoAreaApi.upsertAreasBatch(adm2WithParent)
        }

        countryId
    }

    /**
     * Get all areas for a country.
     * Returns hierarchical structure with children populated.
     */
    suspend fun getAreasByCountry(
        countryCode: String,
        maxLevel: GeoAreaLevel? = null,
    ): List<GeoArea> = withContext(ioDispatcher) {
        val dtos = geoAreaApi.fetchAreasByCountry(countryCode, maxLevel?.value)
        geoAreaMapper.toDomainHierarchy(dtos)
    }

    /**
     * Get all areas at a specific level for a country.
     * Returns flat list (no children populated).
     */
    suspend fun getAreasByLevel(
        countryCode: String,
        level: GeoAreaLevel,
    ): List<GeoArea> = withContext(ioDispatcher) {
        geoAreaApi.fetchAreasByLevel(countryCode, level.value).map { geoAreaMapper.toDomain(it) }
    }

    /**
     * Get direct children of an area.
     */
    suspend fun getChildren(parentId: String): List<GeoArea> = withContext(ioDispatcher) {
        geoAreaApi.fetchChildren(parentId).map { geoAreaMapper.toDomain(it) }
    }

    /**
     * Get area with all descendants.
     */
    suspend fun getAreaWithDescendants(
        areaId: String,
        maxDepth: Int = 10,
    ): List<GeoArea> = withContext(ioDispatcher) {
        val dtos = geoAreaApi.fetchDescendants(areaId, maxDepth)
        geoAreaMapper.toDomainHierarchy(dtos)
    }

    /**
     * Get ancestors of an area (path to root).
     */
    suspend fun getAncestors(areaId: String): List<GeoArea> = withContext(ioDispatcher) {
        geoAreaApi.fetchAncestors(areaId).map { geoAreaMapper.toDomain(it) }
    }

    /**
     * Get a single area by ID.
     */
    suspend fun getAreaById(areaId: String): GeoArea? = withContext(ioDispatcher) {
        geoAreaApi.fetchAreaById(areaId)?.let { geoAreaMapper.toDomain(it) }
    }

    /**
     * Get area with children, with automatic caching.
     * - useCache=true: キャッシュ優先 → キャッシュミス時API取得 → 結果をキャッシュ保存
     */
    suspend fun getAreaByIdWithChildren(
        areaId: String,
        useCache: Boolean = true,
    ): GeoArea? = withContext(ioDispatcher) {
        if (useCache) {
            geoAreaCacheDataSource.load(areaId)?.let { cached ->
                if (cached.children.isNotEmpty()) {
                    return@withContext cached
                }
            }
        }

        val area = geoAreaApi.fetchAreaById(areaId)?.let { geoAreaMapper.toDomain(it) }
            ?: return@withContext null

        val children = geoAreaApi.fetchChildren(areaId).map { geoAreaMapper.toDomain(it) }
        val areaWithChildren = area.copy(children = children)

        if (useCache) {
            geoAreaCacheDataSource.save(areaWithChildren)
        }

        areaWithChildren
    }

    /**
     * Get a single area by adm_id.
     */
    suspend fun getAreaByAdmId(admId: String): GeoArea? = withContext(ioDispatcher) {
        geoAreaApi.fetchAreaByAdmId(admId)?.let { geoAreaMapper.toDomain(it) }
    }

    /**
     * Get all available country codes.
     */
    suspend fun getAvailableCountryCodes(): List<String> = withContext(ioDispatcher) {
        geoAreaApi.fetchDistinctCountryCodes()
    }

    /**
     * Save GeoArea to disk cache.
     */
    suspend fun saveToCache(geoArea: GeoArea) {
        geoAreaCacheDataSource.save(geoArea)
    }

    /**
     * Load GeoArea from disk cache.
     */
    suspend fun getFromCache(geoAreaId: String, maxAge: Duration = 7.days): GeoArea? {
        return geoAreaCacheDataSource.load(geoAreaId, maxAge)
    }

    /**
     * Clear all cached GeoArea data.
     */
    suspend fun clearCache() {
        geoAreaCacheDataSource.clearAll()
    }

    /**
     * Get current cache size in bytes.
     */
    suspend fun getCacheSize(): Long {
        return geoAreaCacheDataSource.getCacheSize()
    }
}
