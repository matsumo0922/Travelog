package me.matsumo.travelog.core.datasource

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.matsumo.travelog.core.model.geo.GeoArea
import java.io.File
import kotlin.time.Duration

@Serializable
private data class CacheEntry(
    val timestamp: Long,
    val geoArea: GeoArea,
)

class GeoAreaCacheDataSourceImpl(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : GeoAreaCacheDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val memoryCache = mutableMapOf<String, CacheEntry>()
    private val mutex = Mutex()

    override suspend fun save(geoArea: GeoArea) = withContext(ioDispatcher) {
        val geoAreaId = geoArea.id ?: return@withContext
        val entry = CacheEntry(
            timestamp = System.currentTimeMillis(),
            geoArea = geoArea,
        )

        mutex.withLock {
            memoryCache[geoAreaId] = entry
        }

        val file = getFile(geoAreaId)
        file.writeText(json.encodeToString(entry))
    }

    override suspend fun load(geoAreaId: String, maxAge: Duration): GeoArea? = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val maxAgeMillis = maxAge.inWholeMilliseconds

        mutex.withLock {
            memoryCache[geoAreaId]?.let { entry ->
                if (now - entry.timestamp <= maxAgeMillis) {
                    return@withContext entry.geoArea
                } else {
                    memoryCache.remove(geoAreaId)
                }
            }
        }

        val file = getFile(geoAreaId)
        if (!file.exists()) return@withContext null

        runCatching {
            val entry = json.decodeFromString<CacheEntry>(file.readText())
            if (now - entry.timestamp <= maxAgeMillis) {
                mutex.withLock {
                    memoryCache[geoAreaId] = entry
                }
                entry.geoArea
            } else {
                file.delete()
                null
            }
        }.getOrNull()
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        mutex.withLock {
            memoryCache.clear()
        }

        val cacheDir = getCacheDir()
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
    }

    override suspend fun getCacheSize(): Long = withContext(ioDispatcher) {
        val cacheDir = getCacheDir()
        if (!cacheDir.exists()) return@withContext 0L

        cacheDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    private fun getCacheDir(): File {
        return File(context.cacheDir, CACHE_DIR_NAME)
    }

    private fun getFile(geoAreaId: String): File {
        val cacheDir = getCacheDir()
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val safeFileName = geoAreaId.replace(Regex("[^a-zA-Z0-9-_]"), "_")
        return File(cacheDir, "$safeFileName.json")
    }

    companion object {
        private const val CACHE_DIR_NAME = "geoarea_cache"
    }
}
