package me.matsumo.travelog.core.datasource

import me.matsumo.travelog.core.model.db.Image
import kotlin.time.Duration

/**
 * No-op implementation for JVM.
 * JVM desktop doesn't need caching for images since it's mainly for testing/development.
 */
class ImageCacheDataSourceImpl : ImageCacheDataSource {

    override suspend fun save(image: Image) {
        // No-op
    }

    override suspend fun saveAll(images: List<Image>) {
        // No-op
    }

    override suspend fun load(imageId: String, maxAge: Duration): Image? {
        return null
    }

    override suspend fun loadAll(imageIds: List<String>, maxAge: Duration): List<Image> {
        return emptyList()
    }

    override suspend fun delete(imageId: String) {
        // No-op
    }

    override suspend fun clearAll() {
        // No-op
    }
}
