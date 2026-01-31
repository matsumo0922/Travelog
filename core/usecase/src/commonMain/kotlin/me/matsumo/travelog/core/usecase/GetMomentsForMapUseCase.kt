package me.matsumo.travelog.core.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.matsumo.travelog.core.datasource.api.StorageApi
import me.matsumo.travelog.core.model.DateRange
import me.matsumo.travelog.core.model.MomentItem
import me.matsumo.travelog.core.model.PreviewImage
import me.matsumo.travelog.core.model.db.Image
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.StorageRepository

/**
 * UseCase for fetching MomentItem list for a map's regions.
 * Each MomentItem contains a region with its GeoArea and preview images.
 */
class GetMomentsForMapUseCase(
    private val geoAreaRepository: GeoAreaRepository,
    private val imageRepository: ImageRepository,
    private val storageRepository: StorageRepository,
) {
    /**
     * Fetch moments for all regions in parallel.
     *
     * @param regions List of MapRegions to fetch moments for
     * @return List of MomentItem sorted by GeoArea's isoCode
     */
    suspend operator fun invoke(regions: List<MapRegion>): List<MomentItem> = coroutineScope {
        regions
            .map { region -> async { buildMomentItem(region) } }
            .awaitAll()
            .filterNotNull()
            .filter { it.totalImageCount > 0 }
            .sortedBy { it.geoArea.isoCode ?: it.geoArea.admId }
    }

    private suspend fun buildMomentItem(region: MapRegion): MomentItem? {
        val regionId = region.id ?: return null

        val geoArea = geoAreaRepository.getAreaById(region.geoAreaId) ?: return null

        val images = imageRepository.getPreviewImagesByMapRegionId(regionId, PREVIEW_IMAGE_LIMIT)
        val totalCount = imageRepository.getImageCountByMapRegionId(regionId)

        val previewImages = coroutineScope {
            images.map { image ->
                async { buildPreviewImage(image) }
            }.awaitAll().filterNotNull()
        }

        val dateRange = calculateDateRange(images)

        return MomentItem(
            mapRegion = region,
            geoArea = geoArea,
            previewImages = previewImages,
            totalImageCount = totalCount,
            dateRange = dateRange,
        )
    }

    private suspend fun buildPreviewImage(image: Image): PreviewImage? {
        val imageId = image.id ?: return null
        val url = getImageUrl(image) ?: return null

        return PreviewImage(
            id = imageId,
            url = url,
            width = image.width,
            height = image.height,
        )
    }

    private suspend fun getImageUrl(image: Image): String? {
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

    private fun calculateDateRange(images: List<Image>): DateRange? {
        val takenDates = images.mapNotNull { it.takenAt }
        if (takenDates.isEmpty()) return null

        return DateRange(
            earliest = takenDates.min(),
            latest = takenDates.max(),
        )
    }

    companion object {
        private const val PREVIEW_IMAGE_LIMIT = 5
    }
}
