package me.matsumo.travelog.core.repository

import me.matsumo.travelog.core.datasource.api.MapApi
import me.matsumo.travelog.core.model.db.Map

class MapRepository(
    private val mapApi: MapApi,
) {
    suspend fun createMap(map: Map) {
        mapApi.createMap(map)
    }

    suspend fun updateMap(map: Map) {
        mapApi.updateMap(map)
    }

    suspend fun getMap(id: String): Map? {
        return mapApi.getMap(id)
    }

    suspend fun getMapsByUserId(userId: String): List<Map> {
        return mapApi.getMapsByUserId(userId)
    }

    suspend fun deleteMap(id: String) {
        mapApi.deleteMap(id)
    }
}
