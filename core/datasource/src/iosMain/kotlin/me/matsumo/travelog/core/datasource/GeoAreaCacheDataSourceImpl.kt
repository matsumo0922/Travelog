package me.matsumo.travelog.core.datasource

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.matsumo.travelog.core.model.geo.GeoArea
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import kotlin.time.Duration

@Serializable
private data class CacheEntry(
    val timestamp: Long,
    val geoArea: GeoArea,
)

class GeoAreaCacheDataSourceImpl(
    private val ioDispatcher: CoroutineDispatcher,
) : GeoAreaCacheDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val memoryCache = mutableMapOf<String, CacheEntry>()
    private val mutex = Mutex()

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun save(geoArea: GeoArea) = withContext(ioDispatcher) {
        val geoAreaId = geoArea.id ?: return@withContext
        val now = (NSDate().timeIntervalSince1970 * 1000).toLong()
        val entry = CacheEntry(
            timestamp = now,
            geoArea = geoArea,
        )

        mutex.withLock {
            memoryCache[geoAreaId] = entry
        }

        val path = getFilePath(geoAreaId)
        val content = json.encodeToString(entry)
        NSString.create(string = content).writeToFile(path, true, NSUTF8StringEncoding, null)
        Unit
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun load(geoAreaId: String, maxAge: Duration): GeoArea? = withContext(ioDispatcher) {
        val now = (NSDate().timeIntervalSince1970 * 1000).toLong()
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

        val path = getFilePath(geoAreaId)
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) {
            return@withContext null
        }

        runCatching {
            val content = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
                ?: return@withContext null
            val entry = json.decodeFromString<CacheEntry>(content)

            if (now - entry.timestamp <= maxAgeMillis) {
                mutex.withLock {
                    memoryCache[geoAreaId] = entry
                }
                entry.geoArea
            } else {
                NSFileManager.defaultManager.removeItemAtPath(path, null)
                null
            }
        }.getOrNull()
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun clearAll() = withContext(ioDispatcher) {
        mutex.withLock {
            memoryCache.clear()
        }

        val dirPath = getCacheDirPath()
        if (NSFileManager.defaultManager.fileExistsAtPath(dirPath)) {
            NSFileManager.defaultManager.removeItemAtPath(dirPath, null)
        }
        Unit
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getCacheSize(): Long = withContext(ioDispatcher) {
        val dirPath = getCacheDirPath()
        if (!NSFileManager.defaultManager.fileExistsAtPath(dirPath)) {
            return@withContext 0L
        }

        var totalSize = 0L
        val contents = NSFileManager.defaultManager.contentsOfDirectoryAtPath(dirPath, null)
        contents?.forEach { fileName ->
            val filePath = "$dirPath/$fileName"
            val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(filePath, null)
            val fileSize = attrs?.get("NSFileSize") as? Long ?: 0L
            totalSize += fileSize
        }
        totalSize
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun getCacheDirPath(): String {
        val cacheDir = NSFileManager.defaultManager.URLForDirectory(
            directory = NSCachesDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        return "${cacheDir!!.path}/$CACHE_DIR_NAME"
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun getFilePath(geoAreaId: String): String {
        val dirPath = getCacheDirPath()
        if (!NSFileManager.defaultManager.fileExistsAtPath(dirPath)) {
            NSFileManager.defaultManager.createDirectoryAtPath(
                path = dirPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }
        val safeFileName = geoAreaId.replace(Regex("[^a-zA-Z0-9-_]"), "_")
        return "$dirPath/$safeFileName.json"
    }

    companion object {
        private const val CACHE_DIR_NAME = "geoarea_cache"
    }
}
