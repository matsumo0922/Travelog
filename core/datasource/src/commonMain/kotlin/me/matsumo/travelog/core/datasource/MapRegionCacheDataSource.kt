package me.matsumo.travelog.core.datasource

import kotlinx.coroutines.flow.SharedFlow
import me.matsumo.travelog.core.model.db.MapRegion

interface MapRegionCacheDataSource {
    /**
     * キャッシュ更新イベントをFlowで配信（mapIdを通知）
     */
    val cacheUpdates: SharedFlow<String>

    suspend fun upsert(region: MapRegion)
    suspend fun upsertAll(regions: List<MapRegion>)
    suspend fun getByMapId(mapId: String): List<MapRegion>
    suspend fun getByMapIdAndGeoAreaId(mapId: String, geoAreaId: String): List<MapRegion>
    suspend fun delete(regionId: String)
    suspend fun clearAll()
}
