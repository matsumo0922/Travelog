package me.matsumo.travelog.core.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.datasource.api.GeoAreaApi
import me.matsumo.travelog.core.datasource.helper.GeoAreaMapper
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.model.geo.GeoAreaLevel

/**
 * Repository for managing geo areas in the database.
 *
 * This repository provides operations for storing and retrieving hierarchical
 * geo area data (ADM0 -> ADM1 -> ADM2).
 */
class GeoAreaRepository(
    private val geoAreaApi: GeoAreaApi,
    private val geoAreaMapper: GeoAreaMapper,
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
     * Get a single area by adm_id and parent_id.
     */
    suspend fun getAreaByAdmId(admId: String, parentId: String? = null): GeoArea? = withContext(ioDispatcher) {
        geoAreaApi.fetchAreaByAdmId(admId, parentId)?.let { geoAreaMapper.toDomain(it) }
    }

    /**
     * Get all available country codes.
     */
    suspend fun getAvailableCountryCodes(): List<String> = withContext(ioDispatcher) {
        geoAreaApi.fetchDistinctCountryCodes()
    }
}
