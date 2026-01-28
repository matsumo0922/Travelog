package me.matsumo.travelog.core.datasource

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.matsumo.travelog.core.model.db.MapRegion

/**
 * No-op implementation for JVM.
 * JVM desktop doesn't need caching for MapRegion since it's mainly for testing/development.
 */
class MapRegionCacheDataSourceImpl : MapRegionCacheDataSource {

    private val _cacheUpdates = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 64)

    override val cacheUpdates: SharedFlow<String> = _cacheUpdates.asSharedFlow()

    override suspend fun upsert(region: MapRegion) {
        // No-op but still emit for consistency
        _cacheUpdates.emit(region.mapId)
    }

    override suspend fun upsertAll(regions: List<MapRegion>) {
        // No-op but still emit for consistency
        regions.map { it.mapId }.toSet().forEach { mapId ->
            _cacheUpdates.emit(mapId)
        }
    }

    override suspend fun getByMapId(mapId: String): List<MapRegion> {
        return emptyList()
    }

    override suspend fun getByMapIdAndGeoAreaId(mapId: String, geoAreaId: String): List<MapRegion> {
        return emptyList()
    }

    override suspend fun delete(regionId: String) {
        // No-op
    }

    override suspend fun clearAll() {
        // No-op
    }
}
