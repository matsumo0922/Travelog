package me.matsumo.travelog.core.usecase

import io.github.aakira.napier.Napier
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.MapRegionRepository

class DeleteRepresentativeImageUseCase(
    private val imageRepository: ImageRepository,
    private val mapRegionRepository: MapRegionRepository,
) {
    suspend operator fun invoke(mapRegion: MapRegion): Result {
        val regionId = mapRegion.id ?: return Result.InvalidRegion

        return runCatching {
            // 1. Delete original image (storage + DB record)
            mapRegion.representativeImageId?.let { imageId ->
                runCatching { imageRepository.deleteImage(imageId) }
            }

            // 2. Delete cropped image (storage + DB record)
            mapRegion.representativeCroppedImageId?.let { imageId ->
                runCatching { imageRepository.deleteImage(imageId) }
            }

            // 3. Update MapRegion (clear representative image fields)
            mapRegionRepository.clearRepresentativeImage(regionId)

            Result.Success
        }.getOrElse { e ->
            Napier.e(e) { "Failed to delete representative image" }
            Result.Failed(e)
        }
    }

    sealed interface Result {
        data object Success : Result
        data object InvalidRegion : Result
        data class Failed(val cause: Throwable) : Result
    }
}
