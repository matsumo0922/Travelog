package me.matsumo.travelog.core.usecase

import androidx.compose.runtime.Stable
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import me.matsumo.travelog.core.model.db.Image
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.MapRegionRepository
import me.matsumo.travelog.core.repository.SessionRepository
import me.matsumo.travelog.core.repository.StorageRepository

class UploadMapAreaImagesUseCase(
    private val sessionRepository: SessionRepository,
    private val mapRegionRepository: MapRegionRepository,
    private val storageRepository: StorageRepository,
    private val imageRepository: ImageRepository,
) {
    operator fun invoke(
        files: List<PlatformFile>,
        mapId: String,
        geoAreaId: String,
    ): Flow<UploadProgress> = flow {
        if (files.isEmpty()) {
            emit(UploadProgress.Completed(0, null))
            return@flow
        }

        val totalCount = files.size
        emit(UploadProgress.Uploading(totalCount, 0))

        val userId = checkNotNull(sessionRepository.getCurrentUserInfo()?.id) {
            "User not logged in"
        }

        val existingRegion = mapRegionRepository.getMapRegionsByMapIdAndGeoAreaId(mapId, geoAreaId)
            .firstOrNull()
        val targetRegion = existingRegion ?: mapRegionRepository.createMapRegion(
            MapRegion(
                mapId = mapId,
                geoAreaId = geoAreaId,
            ),
        )

        val semaphore = Semaphore(MAX_CONCURRENT_UPLOADS)
        var completedCount = 0

        val results = coroutineScope {
            files.map { file ->
                async {
                    semaphore.withPermit {
                        val result = uploadSingleImage(file, userId, targetRegion.id)
                        completedCount++
                        emit(UploadProgress.Uploading(totalCount, completedCount))
                        result
                    }
                }
            }.awaitAll()
        }

        val successResults = results.filterNotNull()
        val singleNavigation = if (totalCount == 1 && successResults.isNotEmpty()) {
            successResults.first()
        } else {
            null
        }

        emit(UploadProgress.Completed(successResults.size, singleNavigation))
    }

    private suspend fun uploadSingleImage(
        file: PlatformFile,
        userId: String,
        mapRegionId: String?,
    ): UploadedImageResult? {
        return runCatching {
            val metadata = extractImageMetadata(file)
            val upload = storageRepository.uploadMapRegionImage(file, userId)
            val image = Image(
                uploaderUserId = userId,
                mapRegionId = mapRegionId,
                storageKey = upload.storageKey,
                contentType = upload.contentType,
                fileSize = upload.fileSize,
                width = metadata?.width,
                height = metadata?.height,
                takenAt = metadata?.takenAt,
                takenLat = metadata?.takenLat,
                takenLng = metadata?.takenLng,
                exif = metadata?.exif,
                bucketName = upload.bucketName,
            )
            val createdImage = imageRepository.createImage(image)
            val imageUrl = storageRepository.getSignedUrl(
                bucketName = upload.bucketName,
                storageKey = upload.storageKey,
            )

            UploadedImageResult(
                imageId = createdImage.id.orEmpty(),
                imageUrl = imageUrl,
            )
        }.getOrNull()
    }

    companion object {
        private const val MAX_CONCURRENT_UPLOADS = 3
    }
}

@Stable
sealed interface UploadProgress {
    data class Uploading(
        val totalCount: Int,
        val completedCount: Int,
    ) : UploadProgress

    data class Completed(
        val successCount: Int,
        val singleImageResult: UploadedImageResult?,
    ) : UploadProgress
}

@Stable
data class UploadedImageResult(
    val imageId: String,
    val imageUrl: String?,
)
