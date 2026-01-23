package me.matsumo.travelog.core.datasource

import me.matsumo.travelog.core.model.geo.GeoArea
import kotlin.time.Duration

/**
 * No-op implementation for JVM.
 * JVM desktop doesn't need disk caching for navigation since
 * there's no serialization issue with navigation arguments.
 */
class GeoAreaCacheDataSourceImpl : GeoAreaCacheDataSource {

    override suspend fun save(geoArea: GeoArea) {
        // No-op
    }

    override suspend fun load(geoAreaId: String, maxAge: Duration): GeoArea? {
        return null
    }

    override suspend fun clearAll() {
        // No-op
    }

    override suspend fun getCacheSize(): Long {
        return 0L
    }
}
