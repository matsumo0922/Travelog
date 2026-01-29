package me.matsumo.travelog.core.usecase

import me.matsumo.travelog.core.datasource.api.StorageApi
import me.matsumo.travelog.core.model.db.Image
import me.matsumo.travelog.core.model.db.ImageComment
import me.matsumo.travelog.core.repository.ImageCommentRepository
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.StorageRepository

class GetPhotoDetailUseCase(
    private val imageRepository: ImageRepository,
    private val imageCommentRepository: ImageCommentRepository,
    private val storageRepository: StorageRepository,
) {
    suspend operator fun invoke(imageId: String, initialImageUrl: String?): PhotoDetailResult {
        val image = imageRepository.getImage(imageId)
        val imageUrl = image?.let { resolveImageUrl(it) } ?: initialImageUrl
        val comments = if (imageId.isNotBlank()) {
            imageCommentRepository.getImageCommentsByImageId(imageId)
        } else {
            emptyList()
        }

        return PhotoDetailResult(
            imageId = imageId,
            imageUrl = imageUrl,
            image = image,
            comments = comments,
        )
    }

    private suspend fun resolveImageUrl(image: Image): String? {
        val bucketName = image.bucketName ?: return null

        return when (bucketName) {
            StorageApi.BUCKET_MAP_REGION_IMAGES -> {
                storageRepository.getSignedUrl(bucketName, image.storageKey)
            }

            else -> {
                storageRepository.getMapIconPublicUrl(image.storageKey)
            }
        }
    }
}

data class PhotoDetailResult(
    val imageId: String,
    val imageUrl: String?,
    val image: Image?,
    val comments: List<ImageComment>,
)
