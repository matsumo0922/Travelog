package me.matsumo.travelog.core.datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

class GeoBoundaryCacheDataSourceImpl(
    private val ioDispatcher: CoroutineDispatcher,
) : GeoBoundaryCacheDataSource {

    override suspend fun save(key: String, text: String) = withContext(ioDispatcher) {
        val file = getFile(key)
        file.writeText(text)
    }

    override suspend fun load(key: String): String? = withContext(ioDispatcher) {
        val file = getFile(key)
        if (file.exists()) {
            file.readText()
        } else {
            null
        }
    }

    override suspend fun exists(key: String): Boolean = withContext(ioDispatcher) {
        getFile(key).exists()
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        val cacheDir = getCacheDir()
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
        Unit
    }

    private fun getCacheDir(): File {
        val tmpDir = File(System.getProperty("java.io.tmpdir"))
        return File(tmpDir, "travelog_geoboundary_cache")
    }

    private fun getFile(key: String): File {
        val cacheDir = getCacheDir()
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, key)
    }
}
