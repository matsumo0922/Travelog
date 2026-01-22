package me.matsumo.travelog.core.repository

import me.matsumo.travelog.core.datasource.api.MapApi
import me.matsumo.travelog.core.model.db.Map

class MapRepository(
    private val mapApi: MapApi,
    private val imageRepository: ImageRepository,
    private val storageRepository: StorageRepository,
) {
    suspend fun createMap(map: Map) {
        mapApi.createMap(map)
    }

    suspend fun updateMap(map: Map) {
        mapApi.updateMap(map)
    }

    suspend fun getMap(id: String): Map? {
        val map = mapApi.getMap(id) ?: return null
        return resolveIconImageUrl(map)
    }

    suspend fun getMapsByUserId(userId: String): List<Map> {
        val maps = mapApi.getMapsByUserId(userId)
        return resolveIconImageUrls(maps)
    }

    private suspend fun resolveIconImageUrl(map: Map): Map {
        val iconImageId = map.iconImageId ?: return map
        val image = imageRepository.getImage(iconImageId) ?: return map
        val url = storageRepository.getMapIconPublicUrl(image.storageKey)
        return map.copy(iconImageUrl = url)
    }

    private suspend fun resolveIconImageUrls(maps: List<Map>): List<Map> {
        val imageIds = maps.mapNotNull { it.iconImageId }
        if (imageIds.isEmpty()) return maps

        val images = imageRepository.getImagesByIds(imageIds)
        val imageMap = images.associateBy { it.id }

        return maps.map { map ->
            val iconImageId = map.iconImageId ?: return@map map
            val image = imageMap[iconImageId] ?: return@map map
            val url = storageRepository.getMapIconPublicUrl(image.storageKey)
            map.copy(iconImageUrl = url)
        }
    }

    suspend fun deleteMap(id: String) {
        mapApi.deleteMap(id)
    }
}
