package me.matsumo.travelog.core.usecase

import android.content.Context
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AndroidTempFileStorage(
    private val context: Context,
) : TempFileStorage {

    private val tempDir: File
        get() = File(context.cacheDir, TEMP_SUBDIR).apply { mkdirs() }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun saveToTemp(file: PlatformFile): String = withContext(Dispatchers.IO) {
        val bytes = file.readBytes()
        val fileName = "temp_${Uuid.random()}.jpg"
        val tempFile = File(tempDir, fileName)
        tempFile.writeBytes(bytes)
        tempFile.absolutePath
    }

    override suspend fun loadFromTemp(path: String): PlatformFile? = withContext(Dispatchers.IO) {
        val file = File(path)
        // Security: Validate path is within temp directory
        if (!isPathWithinTempDir(file)) {
            return@withContext null
        }
        if (file.exists()) {
            PlatformFile(file)
        } else {
            null
        }
    }

    override suspend fun deleteTemp(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        // Security: Only delete files within temp directory
        if (!isPathWithinTempDir(file)) {
            return@withContext false
        }
        file.delete()
    }

    private fun isPathWithinTempDir(file: File): Boolean {
        val canonicalTempDir = tempDir.canonicalPath
        val canonicalFile = file.canonicalPath
        // Ensure file is exactly within tempDir by checking with separator suffix
        return canonicalFile.startsWith(canonicalTempDir + File.separator) ||
            canonicalFile == canonicalTempDir
    }

    companion object {
        private const val TEMP_SUBDIR = "photo_crop_temp"
    }
}
