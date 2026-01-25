package me.matsumo.travelog.feature.map

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.datasource.api.StorageApi
import me.matsumo.travelog.core.model.db.Map
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.MapRegionRepository
import me.matsumo.travelog.core.repository.MapRepository
import me.matsumo.travelog.core.repository.StorageRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState

class MapDetailViewModel(
    private val mapId: String,
    private val mapRepository: MapRepository,
    private val mapRegionRepository: MapRegionRepository,
    private val geoAreaRepository: GeoAreaRepository,
    private val imageRepository: ImageRepository,
    private val storageRepository: StorageRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapDetailUiState>>(ScreenState.Loading())
    val screenState = _screenState.asStateFlow()

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                val map = mapRepository.getMap(mapId) ?: error("Map not found")
                val geoArea = geoAreaRepository.getAreaByIdWithChildren(map.rootGeoAreaId, true) ?: error("Geo area not found")
                val regions = mapRegionRepository.getMapRegionsByMapId(mapId)

                val imageIds = regions.flatMap {
                    listOfNotNull(it.representativeImageId, it.representativeCroppedImageId)
                }.distinct()
                val images = imageRepository.getImagesByIds(imageIds)
                val imageUrlMap = images.mapNotNull { image ->
                    val imageId = image.id ?: return@mapNotNull null
                    val bucketName = image.bucketName
                    val url = when (bucketName) {
                        StorageApi.BUCKET_MAP_REGION_IMAGES -> {
                            storageRepository.getSignedUrl(bucketName, image.storageKey)
                        }

                        else -> {
                            storageRepository.getMapIconPublicUrl(image.storageKey)
                        }
                    }
                    imageId to url
                }.toMap()

                MapDetailUiState(
                    map = map,
                    geoArea = geoArea,
                    regions = regions.toImmutableList(),
                    regionImageUrls = imageUrlMap.toImmutableMap(),
                )
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }
}

@Stable
data class MapDetailUiState(
    val map: Map,
    val geoArea: GeoArea,
    val regions: ImmutableList<MapRegion>,
    val regionImageUrls: ImmutableMap<String, String>,
)
