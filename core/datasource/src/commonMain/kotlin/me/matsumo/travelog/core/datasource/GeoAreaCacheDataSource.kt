package me.matsumo.travelog.core.datasource

import me.matsumo.travelog.core.model.geo.GeoArea
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

interface GeoAreaCacheDataSource {
    suspend fun save(geoArea: GeoArea)
    suspend fun load(geoAreaId: String, maxAge: Duration = 7.days): GeoArea?
    suspend fun clearAll()
    suspend fun getCacheSize(): Long
}
