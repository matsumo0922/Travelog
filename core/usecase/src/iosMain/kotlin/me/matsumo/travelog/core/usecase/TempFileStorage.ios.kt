package me.matsumo.travelog.core.usecase

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class IosTempFileStorage : TempFileStorage {

    private val tempDir: String
        get() = "${getCacheDirectory()}/$TEMP_SUBDIR"

    @OptIn(ExperimentalUuidApi::class, ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun saveToTemp(file: PlatformFile): String = withContext(Dispatchers.IO) {
        val bytes = file.readBytes()
        val fileName = "temp_${Uuid.random()}.jpg"

        // Ensure temp directory exists
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(tempDir)) {
            fileManager.createDirectoryAtPath(tempDir, withIntermediateDirectories = true, attributes = null, error = null)
        }

        val filePath = "$tempDir/$fileName"
        val fileUrl = NSURL.fileURLWithPath(filePath)

        val nsData = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        nsData.writeToURL(fileUrl, atomically = true)

        filePath
    }

    override suspend fun loadFromTemp(path: String): PlatformFile? = withContext(Dispatchers.IO) {
        // Security: Validate path is within temp directory
        if (!isPathWithinTempDir(path)) {
            return@withContext null
        }
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(path)) {
            PlatformFile(NSURL.fileURLWithPath(path))
        } else {
            null
        }
    }

    override suspend fun deleteTemp(path: String): Boolean = withContext(Dispatchers.IO) {
        // Security: Only delete files within temp directory
        if (!isPathWithinTempDir(path)) {
            return@withContext false
        }
        val fileManager = NSFileManager.defaultManager
        runCatching {
            fileManager.removeItemAtPath(path, null)
        }.isSuccess
    }

    private fun isPathWithinTempDir(path: String): Boolean {
        // Normalize paths using NSURL to prevent path traversal attacks
        val tempDirUrl = NSURL.fileURLWithPath(tempDir)
        val pathUrl = NSURL.fileURLWithPath(path)

        val normalizedTempDir = tempDirUrl.standardizedURL?.path ?: tempDir
        val normalizedPath = pathUrl.standardizedURL?.path ?: path

        // Ensure file is exactly within tempDir by checking with separator suffix
        return normalizedPath.startsWith("$normalizedTempDir/") ||
                normalizedPath == normalizedTempDir
    }

    private fun getCacheDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory,
            NSUserDomainMask,
            true,
        )
        return paths.firstOrNull() as? String ?: ""
    }

    companion object {
        private const val TEMP_SUBDIR = "photo_crop_temp"
    }
}
