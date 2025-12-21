package me.matsumo.travelog.core.datasource

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

class GeoBoundaryCacheDataSourceImpl(
    private val context: Context,
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
        val cacheDir = File(context.cacheDir, "geoboundary_cache")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
        Unit
    }

    private fun getFile(key: String): File {
        val cacheDir = File(context.cacheDir, "geoboundary_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, key)
    }
}
