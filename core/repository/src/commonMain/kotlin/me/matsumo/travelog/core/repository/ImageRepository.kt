package me.matsumo.travelog.core.repository

import me.matsumo.travelog.core.datasource.api.ImageApi
import me.matsumo.travelog.core.model.db.Image

class ImageRepository(
    private val imageApi: ImageApi,
) {
    suspend fun createImage(image: Image): Image {
        return imageApi.createImage(image)
    }

    suspend fun getImage(id: String): Image? {
        return imageApi.getImage(id)
    }

    suspend fun getImagesByMapRegionId(mapRegionId: String): List<Image> {
        return imageApi.getImagesByMapRegionId(mapRegionId)
    }

    suspend fun getImagesByUploaderUserId(userId: String): List<Image> {
        return imageApi.getImagesByUploaderUserId(userId)
    }

    suspend fun deleteImage(id: String) {
        imageApi.deleteImage(id)
    }
}
