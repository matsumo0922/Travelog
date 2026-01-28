package me.matsumo.travelog.core.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.db.MapRegion

class MapRegionCacheDataSourceImpl(
    private val ioDispatcher: CoroutineDispatcher,
) : MapRegionCacheDataSource {

    private val memoryCache = mutableMapOf<String, MapRegion>()
    private val mutex = Mutex()
    private val _cacheUpdates = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 64)

    override val cacheUpdates: SharedFlow<String> = _cacheUpdates.asSharedFlow()

    override suspend fun upsert(region: MapRegion) = withContext(ioDispatcher) {
        val regionId = region.id ?: return@withContext
        mutex.withLock {
            memoryCache[regionId] = region
        }
        _cacheUpdates.emit(region.mapId)
    }

    override suspend fun upsertAll(regions: List<MapRegion>) = withContext(ioDispatcher) {
        val mapIds = mutableSetOf<String>()
        mutex.withLock {
            regions.forEach { region ->
                val regionId = region.id ?: return@forEach
                memoryCache[regionId] = region
                mapIds.add(region.mapId)
            }
        }
        mapIds.forEach { mapId ->
            _cacheUpdates.emit(mapId)
        }
    }

    override suspend fun getByMapId(mapId: String): List<MapRegion> = withContext(ioDispatcher) {
        mutex.withLock {
            memoryCache.values.filter { it.mapId == mapId }
        }
    }

    override suspend fun getByMapIdAndGeoAreaId(
        mapId: String,
        geoAreaId: String,
    ): List<MapRegion> = withContext(ioDispatcher) {
        mutex.withLock {
            memoryCache.values.filter { it.mapId == mapId && it.geoAreaId == geoAreaId }
        }
    }

    override suspend fun delete(regionId: String) {
        withContext(ioDispatcher) {
            val mapId = mutex.withLock {
                val region = memoryCache.remove(regionId)
                region?.mapId
            }
            mapId?.let { _cacheUpdates.emit(it) }
        }
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        mutex.withLock {
            memoryCache.clear()
        }
    }
}
