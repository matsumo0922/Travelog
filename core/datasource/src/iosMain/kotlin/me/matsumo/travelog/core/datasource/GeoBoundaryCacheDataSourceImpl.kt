package me.matsumo.travelog.core.datasource

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

class GeoBoundaryCacheDataSourceImpl(
    private val ioDispatcher: CoroutineDispatcher,
) : GeoBoundaryCacheDataSource {

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun save(key: String, text: String) = withContext(ioDispatcher) {
        val path = getFilePath(key)
        NSString.create(string = text).writeToFile(path, true, NSUTF8StringEncoding, null)
        Unit
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun load(key: String): String? = withContext(ioDispatcher) {
        val path = getFilePath(key)
        if (NSFileManager.defaultManager.fileExistsAtPath(path)) {
            NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
        } else {
            null
        }
    }

    override suspend fun exists(key: String): Boolean = withContext(ioDispatcher) {
        val path = getFilePath(key)
        NSFileManager.defaultManager.fileExistsAtPath(path)
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun clear() = withContext(ioDispatcher) {
        val cacheDir = NSFileManager.defaultManager.URLForDirectory(
            directory = NSCachesDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        val dirPath = "${cacheDir!!.path}/geoboundary_cache"
        if (NSFileManager.defaultManager.fileExistsAtPath(dirPath)) {
            NSFileManager.defaultManager.removeItemAtPath(dirPath, null)
        }
        Unit
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun getFilePath(key: String): String {
        val cacheDir = NSFileManager.defaultManager.URLForDirectory(
            directory = NSCachesDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        val dirPath = "${cacheDir!!.path}/geoboundary_cache"
        if (!NSFileManager.defaultManager.fileExistsAtPath(dirPath)) {
            NSFileManager.defaultManager.createDirectoryAtPath(
                path = dirPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }
        return "$dirPath/$key"
    }
}
