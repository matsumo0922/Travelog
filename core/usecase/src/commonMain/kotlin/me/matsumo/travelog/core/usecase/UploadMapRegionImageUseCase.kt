package me.matsumo.travelog.core.usecase

import io.github.vinceglb.filekit.PlatformFile
import me.matsumo.travelog.core.datasource.api.StorageApi
import me.matsumo.travelog.core.model.db.Image
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.StorageRepository

class UploadMapRegionImageUseCase(
    private val storageRepository: StorageRepository,
    private val imageRepository: ImageRepository,
) {
    suspend operator fun invoke(
        file: PlatformFile,
        userId: String,
    ): UploadMapRegionImageResult {
        val metadata = extractImageMetadata(file)

        val uploadResult = storageRepository.uploadMapRegionImage(file, userId)

        val image = Image(
            uploaderUserId = userId,
            mapRegionId = null,
            storageKey = uploadResult.storageKey,
            contentType = uploadResult.contentType,
            fileSize = uploadResult.fileSize,
            width = metadata?.width,
            height = metadata?.height,
            takenAt = metadata?.takenAt,
            takenLat = metadata?.takenLat,
            takenLng = metadata?.takenLng,
            exif = metadata?.exif,
            bucketName = StorageApi.BUCKET_MAP_REGION_IMAGES,
        )
        val createdImage = imageRepository.createImage(image)

        return UploadMapRegionImageResult(
            imageId = createdImage.id!!,
            storageKey = uploadResult.storageKey,
            bucketName = uploadResult.bucketName,
        )
    }
}

data class UploadMapRegionImageResult(
    val imageId: String,
    val storageKey: String,
    val bucketName: String,
)
