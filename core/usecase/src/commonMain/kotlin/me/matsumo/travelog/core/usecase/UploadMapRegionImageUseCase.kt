package me.matsumo.travelog.core.usecase

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import me.matsumo.travelog.core.datasource.api.StorageApi
import me.matsumo.travelog.core.model.db.CropData
import me.matsumo.travelog.core.model.db.Image
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.StorageRepository

class UploadMapRegionImageUseCase(
    private val storageRepository: StorageRepository,
    private val imageRepository: ImageRepository,
) {
    suspend operator fun invoke(
        file: PlatformFile,
        geoArea: GeoArea,
        cropData: CropData,
        userId: String,
    ): UploadMapRegionImageResult {
        val originalBytes = file.readBytes()
        val metadata = extractImageMetadata(file)

        // 1. Upload original image
        val originalUpload = storageRepository.uploadMapRegionImageBytes(
            bytes = originalBytes,
            userId = userId,
            suffix = "_original",
        )
        val originalImage = Image(
            uploaderUserId = userId,
            mapRegionId = null,
            storageKey = originalUpload.storageKey,
            contentType = originalUpload.contentType,
            fileSize = originalUpload.fileSize,
            width = metadata?.width,
            height = metadata?.height,
            takenAt = metadata?.takenAt,
            takenLat = metadata?.takenLat,
            takenLng = metadata?.takenLng,
            exif = metadata?.exif,
            bucketName = StorageApi.BUCKET_MAP_REGION_IMAGES,
        )
        val createdOriginalImage = imageRepository.createImage(originalImage)

        // 2. Generate and upload cropped image
        val croppedBytes = generateCroppedImage(
            imageBytes = originalBytes,
            geoArea = geoArea,
            cropData = cropData,
        )
        val croppedUpload = storageRepository.uploadMapRegionImageBytes(
            bytes = croppedBytes,
            userId = userId,
            suffix = "_cropped",
        )
        val croppedImage = Image(
            uploaderUserId = userId,
            mapRegionId = null,
            storageKey = croppedUpload.storageKey,
            contentType = croppedUpload.contentType,
            fileSize = croppedUpload.fileSize,
            width = 512,
            height = 512,
            takenAt = metadata?.takenAt,
            takenLat = metadata?.takenLat,
            takenLng = metadata?.takenLng,
            exif = null,
            bucketName = StorageApi.BUCKET_MAP_REGION_IMAGES,
        )
        val createdCroppedImage = imageRepository.createImage(croppedImage)

        return UploadMapRegionImageResult(
            originalImageId = createdOriginalImage.id!!,
            croppedImageId = createdCroppedImage.id!!,
        )
    }
}

data class UploadMapRegionImageResult(
    val originalImageId: String,
    val croppedImageId: String,
)
