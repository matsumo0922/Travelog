package me.matsumo.travelog.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import me.matsumo.travelog.core.datasource.MapRegionCacheDataSource
import me.matsumo.travelog.core.datasource.api.MapRegionApi
import me.matsumo.travelog.core.model.db.MapRegion

class MapRegionRepository(
    private val mapRegionApi: MapRegionApi,
    private val cacheDataSource: MapRegionCacheDataSource,
) {
    /**
     * 指定mapIdのMapRegion一覧をFlowで監視
     * キャッシュの更新を検知してUIに自動反映
     */
    fun observeMapRegionsByMapId(mapId: String): Flow<List<MapRegion>> {
        return cacheDataSource.cacheUpdates
            .filter { it == mapId }
            .map { cacheDataSource.getByMapId(mapId) }
            .onStart { emit(getMapRegionsByMapId(mapId)) }
    }

    /**
     * 指定mapIdとgeoAreaIdのMapRegion一覧をFlowで監視
     */
    fun observeMapRegionsByMapIdAndGeoAreaId(
        mapId: String,
        geoAreaId: String,
    ): Flow<List<MapRegion>> {
        return cacheDataSource.cacheUpdates
            .filter { it == mapId }
            .map { cacheDataSource.getByMapIdAndGeoAreaId(mapId, geoAreaId) }
            .onStart { emit(getMapRegionsByMapIdAndGeoAreaId(mapId, geoAreaId)) }
    }

    suspend fun createMapRegion(mapRegion: MapRegion): MapRegion {
        val created = mapRegionApi.createMapRegion(mapRegion)
        cacheDataSource.upsert(created)
        return created
    }

    suspend fun updateMapRegion(mapRegion: MapRegion): MapRegion {
        val updated = mapRegionApi.updateMapRegion(mapRegion)
        cacheDataSource.upsert(updated)
        return updated
    }

    suspend fun getMapRegion(id: String): MapRegion? {
        return mapRegionApi.getMapRegion(id)
    }

    suspend fun getMapRegionsByMapId(mapId: String): List<MapRegion> {
        val regions = mapRegionApi.getMapRegionsByMapId(mapId)
        cacheDataSource.upsertAll(regions)
        return regions
    }

    suspend fun getMapRegionsByMapIdAndGeoAreaId(
        mapId: String,
        geoAreaId: String,
    ): List<MapRegion> {
        val regions = mapRegionApi.getMapRegionsByMapIdAndGeoAreaId(mapId, geoAreaId)
        cacheDataSource.upsertAll(regions)
        return regions
    }

    /**
     * 強制リフレッシュ（Pull-to-refresh用）
     * APIから最新データを取得してキャッシュを更新
     */
    suspend fun refreshMapRegionsByMapId(mapId: String): List<MapRegion> {
        return getMapRegionsByMapId(mapId)
    }

    suspend fun deleteMapRegion(id: String) {
        mapRegionApi.deleteMapRegion(id)
        cacheDataSource.delete(id)
    }
}
