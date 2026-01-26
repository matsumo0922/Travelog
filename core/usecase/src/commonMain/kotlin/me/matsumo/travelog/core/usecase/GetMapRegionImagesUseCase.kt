package me.matsumo.travelog.core.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.matsumo.travelog.core.datasource.api.StorageApi
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.StorageRepository

class GetMapRegionImagesUseCase(
    private val imageRepository: ImageRepository,
    private val storageRepository: StorageRepository,
) {
    /**
     * MapRegionのリストから画像URLのMapを取得する
     *
     * @param regions MapRegionのリスト
     * @return imageId to URL のMap
     */
    suspend operator fun invoke(regions: List<MapRegion>): Map<String, String> {
        val imageIds = regions.flatMap {
            listOfNotNull(it.representativeImageId, it.representativeCroppedImageId)
        }.distinct()

        if (imageIds.isEmpty()) {
            return emptyMap()
        }

        val images = imageRepository.getImagesByIds(imageIds)

        // 各画像の署名付きURL取得を並列実行
        return coroutineScope {
            images.mapNotNull { image ->
                val imageId = image.id ?: return@mapNotNull null
                async {
                    val url = when (val bucketName = image.bucketName) {
                        StorageApi.BUCKET_MAP_REGION_IMAGES -> {
                            storageRepository.getSignedUrl(bucketName, image.storageKey)
                        }

                        else -> {
                            storageRepository.getMapIconPublicUrl(image.storageKey)
                        }
                    }
                    imageId to url
                }
            }.awaitAll().toMap()
        }
    }
}
