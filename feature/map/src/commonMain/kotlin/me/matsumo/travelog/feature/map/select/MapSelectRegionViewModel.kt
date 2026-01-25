package me.matsumo.travelog.feature.map.select

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.datasource.api.StorageApi
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.MapRegionRepository
import me.matsumo.travelog.core.repository.StorageRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState

class MapSelectRegionViewModel(
    private val mapId: String,
    private val geoAreaId: String,
    private val geoAreaRepository: GeoAreaRepository,
    private val mapRegionRepository: MapRegionRepository,
    private val imageRepository: ImageRepository,
    private val storageRepository: StorageRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapSelectRegionUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<MapSelectRegionUiState>> = _screenState.asStateFlow()

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                val geoArea = geoAreaRepository.getAreaByIdWithChildren(geoAreaId)!!
                val mapRegions = mapRegionRepository.getMapRegionsByMapId(mapId)

                val imageIds = mapRegions.flatMap {
                    listOfNotNull(it.representativeImageId, it.representativeCroppedImageId)
                }.distinct()

                val images = imageRepository.getImagesByIds(imageIds)
                val imageUrlMap = images.mapNotNull { image ->
                    val imageId = image.id ?: return@mapNotNull null
                    val url = when (val bucketName = image.bucketName) {
                        StorageApi.BUCKET_MAP_REGION_IMAGES -> {
                            storageRepository.getSignedUrl(bucketName, image.storageKey)
                        }

                        else -> {
                            storageRepository.getMapIconPublicUrl(image.storageKey)
                        }
                    }
                    imageId to url
                }.toMap()

                val sortedChildren = withContext(ioDispatcher) {
                    geoArea.children.sortedWith(
                        compareBy(
                            { it.isoCode == null },
                            { it.isoCode?.substringAfter("-")?.toIntOrNull() ?: Int.MAX_VALUE },
                            { it.name },
                        ),
                    )
                }

                MapSelectRegionUiState(
                    mapId = mapId,
                    geoArea = geoArea,
                    sortedChildren = sortedChildren.toImmutableList(),
                    mapRegions = mapRegions.toImmutableList(),
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
data class MapSelectRegionUiState(
    val mapId: String,
    val geoArea: GeoArea,
    val sortedChildren: ImmutableList<GeoArea>,
    val mapRegions: ImmutableList<MapRegion>,
    val regionImageUrls: ImmutableMap<String, String> = persistentMapOf(),
)
