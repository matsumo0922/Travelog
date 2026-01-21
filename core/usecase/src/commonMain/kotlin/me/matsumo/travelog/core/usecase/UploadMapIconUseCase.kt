package me.matsumo.travelog.core.usecase

import io.github.vinceglb.filekit.PlatformFile
import me.matsumo.travelog.core.model.db.Image
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.StorageRepository
import kotlin.uuid.ExperimentalUuidApi

class UploadMapIconUseCase(
    private val storageRepository: StorageRepository,
    private val imageRepository: ImageRepository,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        file: PlatformFile,
        userId: String,
    ): UploadMapIconResult {
        // Extract image metadata
        val metadata = extractImageMetadata(file)

        // Upload to storage
        val uploadResult = storageRepository.uploadMapIcon(file, userId)

        // Create image record in database
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
        )
        imageRepository.createImage(image)

        return UploadMapIconResult(
            storageKey = uploadResult.storageKey,
            publicUrl = uploadResult.publicUrl,
        )
    }
}

data class UploadMapIconResult(
    val storageKey: String,
    val publicUrl: String,
)
