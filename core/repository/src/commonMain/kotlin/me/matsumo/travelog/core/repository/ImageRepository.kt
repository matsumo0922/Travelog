package me.matsumo.travelog.core.repository

import me.matsumo.travelog.core.datasource.ImageCacheDataSource
import me.matsumo.travelog.core.datasource.api.ImageApi
import me.matsumo.travelog.core.model.db.Image

class ImageRepository(
    private val imageApi: ImageApi,
    private val imageCacheDataSource: ImageCacheDataSource,
) {
    suspend fun createImage(image: Image): Image {
        return imageApi.createImage(image).also {
            imageCacheDataSource.save(it)
        }
    }

    suspend fun getImage(id: String): Image? {
        // キャッシュを優先的に参照
        imageCacheDataSource.load(id)?.let { return it }

        // キャッシュミス時は API から取得してキャッシュに保存
        return imageApi.getImage(id)?.also {
            imageCacheDataSource.save(it)
        }
    }

    suspend fun getImagesByIds(ids: List<String>): List<Image> {
        if (ids.isEmpty()) return emptyList()

        // キャッシュから取得できるものを取得
        val cachedImages = imageCacheDataSource.loadAll(ids)
        val cachedIds = cachedImages.mapNotNull { it.id }.toSet()

        // キャッシュにないIDのみをAPIから取得
        val missingIds = ids.filter { it !in cachedIds }
        if (missingIds.isEmpty()) {
            return cachedImages
        }

        // APIから取得してキャッシュに保存
        val fetchedImages = imageApi.getImagesByIds(missingIds)
        imageCacheDataSource.saveAll(fetchedImages)

        return cachedImages + fetchedImages
    }

    suspend fun getImagesByMapRegionId(mapRegionId: String): List<Image> {
        return imageApi.getImagesByMapRegionId(mapRegionId).also {
            imageCacheDataSource.saveAll(it)
        }
    }

    suspend fun getImagesByUploaderUserId(userId: String): List<Image> {
        return imageApi.getImagesByUploaderUserId(userId).also {
            imageCacheDataSource.saveAll(it)
        }
    }

    suspend fun deleteImage(id: String) {
        imageApi.deleteImage(id)
    }
}
