package datasource

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

class GeoBoundaryCacheDataSource(
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun save(key: String, text: String) = withContext(ioDispatcher) {
        val file = getFile(key)
        file.writeText(text)
    }

    suspend fun load(key: String): String? = withContext(ioDispatcher) {
        val file = getFile(key)
        if (file.exists()) {
            file.readText()
        } else {
            null
        }
    }

    suspend fun exists(key: String): Boolean = withContext(ioDispatcher) {
        getFile(key).exists()
    }

    suspend fun clear() = withContext(ioDispatcher) {
        val cacheDir = getCacheDir()
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
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
