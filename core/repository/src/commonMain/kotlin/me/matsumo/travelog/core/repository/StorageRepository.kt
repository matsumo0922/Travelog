package me.matsumo.travelog.core.repository

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import me.matsumo.travelog.core.datasource.api.StorageApi
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StorageRepository(
    private val storageApi: StorageApi,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend fun uploadMapIcon(file: PlatformFile, userId: String): UploadResult {
        val bytes = file.readBytes()
        val extension = file.name.substringAfterLast(".", "jpg")
        val contentType = when (extension.lowercase()) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        val fileName = "${Uuid.random()}.$extension"
        val path = "$userId/$fileName"

        val publicUrl = storageApi.uploadImage(
            bucketName = StorageApi.BUCKET_MAP_ICONS,
            path = path,
            data = bytes,
            contentType = contentType,
        )

        return UploadResult(
            storageKey = path,
            publicUrl = publicUrl,
            contentType = contentType,
            fileSize = bytes.size.toLong(),
            bucketName = StorageApi.BUCKET_MAP_ICONS,
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun uploadMapRegionImage(file: PlatformFile, userId: String): UploadResult {
        val bytes = file.readBytes()
        val extension = file.name.substringAfterLast(".", "jpg")
        val contentType = when (extension.lowercase()) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        val fileName = "${Uuid.random()}.$extension"
        val path = "$userId/$fileName"

        storageApi.uploadImage(
            bucketName = StorageApi.BUCKET_MAP_REGION_IMAGES,
            path = path,
            data = bytes,
            contentType = contentType,
        )

        return UploadResult(
            storageKey = path,
            publicUrl = null,
            contentType = contentType,
            fileSize = bytes.size.toLong(),
            bucketName = StorageApi.BUCKET_MAP_REGION_IMAGES,
        )
    }

    suspend fun deleteMapIcon(storageKey: String) {
        storageApi.deleteImage(StorageApi.BUCKET_MAP_ICONS, storageKey)
    }

    suspend fun deleteMapRegionImage(storageKey: String) {
        storageApi.deleteImage(StorageApi.BUCKET_MAP_REGION_IMAGES, storageKey)
    }

    fun getMapIconPublicUrl(storageKey: String): String {
        return storageApi.getPublicUrl(StorageApi.BUCKET_MAP_ICONS, storageKey)
    }

    suspend fun getSignedUrl(bucketName: String, storageKey: String): String {
        return storageApi.createSignedUrl(bucketName, storageKey, 1.hours)
    }
}

data class UploadResult(
    val storageKey: String,
    val publicUrl: String?,
    val contentType: String,
    val fileSize: Long,
    val bucketName: String,
)
