package me.matsumo.travelog.feature.map.area

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.db.Image
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.MapRegionRepository
import me.matsumo.travelog.core.repository.SessionRepository
import me.matsumo.travelog.core.repository.StorageRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.component.PlacedTileItem
import me.matsumo.travelog.core.ui.component.TileGridConfig
import me.matsumo.travelog.core.ui.component.TileGridPlacer
import me.matsumo.travelog.core.ui.screen.ScreenState
import me.matsumo.travelog.core.usecase.GetMapRegionImagesUseCase
import me.matsumo.travelog.core.usecase.extractImageMetadata
import me.matsumo.travelog.feature.map.area.components.MockPhotoGenerator
import me.matsumo.travelog.feature.map.area.components.model.GridPhotoItem

class MapAreaDetailViewModel(
    private val mapId: String,
    private val geoAreaId: String,
    private val initialRegions: List<MapRegion>?,
    private val initialRegionImageUrls: Map<String, String>?,
    private val geoAreaRepository: GeoAreaRepository,
    private val mapRegionRepository: MapRegionRepository,
    private val getMapRegionImagesUseCase: GetMapRegionImagesUseCase,
    private val sessionRepository: SessionRepository,
    private val storageRepository: StorageRepository,
    private val imageRepository: ImageRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapAreaDetailUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<MapAreaDetailUiState>> = _screenState.asStateFlow()

    private val _navigateToPhotoDetail = MutableSharedFlow<PhotoDetailNavigation>()
    val navigateToPhotoDetail: SharedFlow<PhotoDetailNavigation> = _navigateToPhotoDetail.asSharedFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val gridConfig = TileGridConfig()

    init {
        fetch()
        observeMapRegions()
    }

    private fun observeMapRegions() {
        viewModelScope.launch {
            mapRegionRepository.observeMapRegionsByMapIdAndGeoAreaId(mapId, geoAreaId)
                .collectLatest { regions ->
                    val currentState = _screenState.value
                    if (currentState is ScreenState.Idle) {
                        val imageUrlMap = getMapRegionImagesUseCase(regions)
                        _screenState.value = ScreenState.Idle(
                            currentState.data.copy(
                                mapRegions = regions.toImmutableList(),
                                regionImageUrls = imageUrlMap.toImmutableMap(),
                            )
                        )
                    }
                }
        }
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                val geoArea = geoAreaRepository.getAreaByIdWithChildren(geoAreaId)
                val mapRegions = initialRegions?.filter { it.geoAreaId == geoAreaId }
                    ?: mapRegionRepository.getMapRegionsByMapIdAndGeoAreaId(mapId, geoAreaId)

                // 初期データがあればそれを優先使用、なければUseCaseから取得
                val imageUrlMap = initialRegionImageUrls ?: getMapRegionImagesUseCase(mapRegions)

                val mockPhotos = MockPhotoGenerator.generateMockPhotos(
                    count = 500,
                    config = gridConfig,
                )

                val placementResult = withContext(Dispatchers.Default) {
                    val placer = TileGridPlacer(gridConfig)
                    placer.placeItems(mockPhotos)
                }

                MapAreaDetailUiState(
                    geoArea = geoArea!!,
                    mapRegions = mapRegions.toImmutableList(),
                    regionImageUrls = imageUrlMap.toImmutableMap(),
                    placedItems = placementResult.placedItems.toImmutableList(),
                    rowCount = placementResult.rowCount,
                )
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }

    fun uploadImage(file: PlatformFile) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val userId = sessionRepository.getCurrentUserInfo()?.id ?: return@launch
                val metadata = extractImageMetadata(file)

                val result = suspendRunCatching {
                    val upload = storageRepository.uploadMapRegionImage(file, userId)
                    val image = Image(
                        uploaderUserId = userId,
                        mapRegionId = null,
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

                    PhotoDetailNavigation(
                        imageId = createdImage.id.orEmpty(),
                        imageUrl = imageUrl,
                    )
                }

                result.onSuccess { event ->
                    if (event.imageId.isNotBlank()) {
                        _navigateToPhotoDetail.emit(event)
                    }
                }
            } finally {
                _isUploading.value = false
            }
        }
    }
}

@Stable
data class MapAreaDetailUiState(
    val geoArea: GeoArea,
    val mapRegions: ImmutableList<MapRegion>,
    val regionImageUrls: ImmutableMap<String, String> = persistentMapOf(),
    val placedItems: ImmutableList<PlacedTileItem<GridPhotoItem>> = persistentListOf(),
    val rowCount: Int = 0,
)

@Stable
data class PhotoDetailNavigation(
    val imageId: String,
    val imageUrl: String?,
)
