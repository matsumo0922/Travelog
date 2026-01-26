package me.matsumo.travelog.core.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.db.Image
import kotlin.time.Duration

private data class ImageCacheEntry(
    val timestamp: Long,
    val image: Image,
)

class ImageCacheDataSourceImpl(
    private val ioDispatcher: CoroutineDispatcher,
) : ImageCacheDataSource {

    private val memoryCache = mutableMapOf<String, ImageCacheEntry>()
    private val mutex = Mutex()

    override suspend fun save(image: Image) = withContext(ioDispatcher) {
        val imageId = image.id ?: return@withContext
        val entry = ImageCacheEntry(
            timestamp = System.currentTimeMillis(),
            image = image,
        )

        mutex.withLock {
            memoryCache[imageId] = entry
        }
    }

    override suspend fun saveAll(images: List<Image>) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        mutex.withLock {
            images.forEach { image ->
                val imageId = image.id ?: return@forEach
                memoryCache[imageId] = ImageCacheEntry(
                    timestamp = now,
                    image = image,
                )
            }
        }
    }

    override suspend fun load(imageId: String, maxAge: Duration): Image? = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val maxAgeMillis = maxAge.inWholeMilliseconds

        mutex.withLock {
            memoryCache[imageId]?.let { entry ->
                if (now - entry.timestamp <= maxAgeMillis) {
                    return@withContext entry.image
                } else {
                    memoryCache.remove(imageId)
                }
            }
        }
        null
    }

    override suspend fun loadAll(imageIds: List<String>, maxAge: Duration): List<Image> = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val maxAgeMillis = maxAge.inWholeMilliseconds
        val result = mutableListOf<Image>()
        val expiredKeys = mutableListOf<String>()

        mutex.withLock {
            imageIds.forEach { imageId ->
                memoryCache[imageId]?.let { entry ->
                    if (now - entry.timestamp <= maxAgeMillis) {
                        result.add(entry.image)
                    } else {
                        expiredKeys.add(imageId)
                    }
                }
            }
            expiredKeys.forEach { memoryCache.remove(it) }
        }
        result
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        mutex.withLock {
            memoryCache.clear()
        }
    }
}
