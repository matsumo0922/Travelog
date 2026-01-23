package me.matsumo.travelog.core.repository

import me.matsumo.travelog.core.datasource.api.MapRegionApi
import me.matsumo.travelog.core.model.db.MapRegion

class MapRegionRepository(
    private val mapRegionApi: MapRegionApi,
) {
    suspend fun createMapRegion(mapRegion: MapRegion) {
        mapRegionApi.createMapRegion(mapRegion)
    }

    suspend fun updateMapRegion(mapRegion: MapRegion) {
        mapRegionApi.updateMapRegion(mapRegion)
    }

    suspend fun getMapRegion(id: String): MapRegion? {
        return mapRegionApi.getMapRegion(id)
    }

    suspend fun getMapRegionsByMapId(mapId: String): List<MapRegion> {
        return mapRegionApi.getMapRegionsByMapId(mapId)
    }

    suspend fun getMapRegionsByMapIdAndGeoAreaId(
        mapId: String,
        geoAreaId: String,
    ): List<MapRegion> {
        return mapRegionApi.getMapRegionsByMapIdAndGeoAreaId(mapId, geoAreaId)
    }

    suspend fun deleteMapRegion(id: String) {
        mapRegionApi.deleteMapRegion(id)
    }
}
