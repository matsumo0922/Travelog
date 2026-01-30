package me.matsumo.travelog.feature.map.area

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import me.matsumo.travelog.core.datasource.api.StorageApi
import me.matsumo.travelog.core.model.db.Image
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.MapRegionRepository
import me.matsumo.travelog.core.repository.StorageRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.component.PlacedTileItem
import me.matsumo.travelog.core.ui.component.TileGridConfig
import me.matsumo.travelog.core.ui.component.TileGridPlacementResult
import me.matsumo.travelog.core.ui.component.TileGridPlacer
import me.matsumo.travelog.core.ui.component.TileSpanSize
import me.matsumo.travelog.core.ui.screen.ScreenState
import me.matsumo.travelog.core.usecase.DeleteMapAreaImagesUseCase
import me.matsumo.travelog.core.usecase.DeleteProgress
import me.matsumo.travelog.core.usecase.GetMapRegionImagesUseCase
import me.matsumo.travelog.core.usecase.UploadMapAreaImagesUseCase
import me.matsumo.travelog.core.usecase.UploadProgress
import me.matsumo.travelog.feature.map.area.components.model.GridPhotoItem

class MapAreaDetailViewModel(
    private val mapId: String,
    private val geoAreaId: String,
    private val initialRegions: List<MapRegion>?,
    private val initialRegionImageUrls: Map<String, String>?,
    private val geoAreaRepository: GeoAreaRepository,
    private val mapRegionRepository: MapRegionRepository,
    private val getMapRegionImagesUseCase: GetMapRegionImagesUseCase,
    private val uploadMapAreaImagesUseCase: UploadMapAreaImagesUseCase,
    private val deleteMapAreaImagesUseCase: DeleteMapAreaImagesUseCase,
    private val storageRepository: StorageRepository,
    private val imageRepository: ImageRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapAreaDetailUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<MapAreaDetailUiState>> = _screenState.asStateFlow()

    private val _navigateToPhotoDetail = MutableSharedFlow<PhotoDetailNavigation>()
    val navigateToPhotoDetail: SharedFlow<PhotoDetailNavigation> = _navigateToPhotoDetail.asSharedFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _selectionState = MutableStateFlow(SelectionState())
    val selectionState: StateFlow<SelectionState> = _selectionState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

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
                        val placementResult = buildPlacedItems(regions)
                        _screenState.value = ScreenState.Idle(
                            currentState.data.copy(
                                mapRegions = regions.toImmutableList(),
                                regionImageUrls = imageUrlMap.toImmutableMap(),
                                placedItems = placementResult.placedItems.toImmutableList(),
                                rowCount = placementResult.rowCount,
                            ),
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

                val placementResult = buildPlacedItems(mapRegions)

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

    private suspend fun buildPlacedItems(mapRegions: List<MapRegion>): TileGridPlacementResult<GridPhotoItem> {
        val mapRegionIds = mapRegions.mapNotNull { it.id }
        val images = coroutineScope {
            mapRegionIds.map { regionId ->
                async { imageRepository.getImagesByMapRegionId(regionId) }
            }.awaitAll().flatten()
        }

        val items = coroutineScope {
            images.mapIndexed { index, image ->
                async {
                    val imageId = image.id ?: return@async null
                    val imageUrl = resolveImageUrl(image) ?: return@async null
                    val useSpecialSize = gridConfig.specialSizeInterval > 0 &&
                        (index + 1) % gridConfig.specialSizeInterval == 0
                    val span = if (useSpecialSize) resolveSpanSize(image) else TileSpanSize(1, 1)
                    GridPhotoItem(
                        id = imageId,
                        imageUrl = imageUrl,
                        spanWidth = span.spanWidth,
                        spanHeight = span.spanHeight,
                    )
                }
            }.awaitAll().filterNotNull()
        }

        return withContext(Dispatchers.Default) {
            val placer = TileGridPlacer(gridConfig)
            placer.placeItems(items)
        }
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

    private fun resolveSpanSize(image: Image): TileSpanSize {
        val width = image.width
        val height = image.height
        if (width == null || height == null || width <= 0 || height <= 0) {
            return TileSpanSize(1, 1)
        }

        val ratio = width.toFloat() / height.toFloat()
        val candidates = when {
            ratio >= 1.6f -> listOf(TileSpanSize(3, 2), TileSpanSize(2, 1))
            ratio >= 1.2f -> listOf(TileSpanSize(2, 1))
            ratio <= 0.65f -> listOf(TileSpanSize(2, 3), TileSpanSize(1, 2))
            ratio <= 0.85f -> listOf(TileSpanSize(1, 2))
            else -> listOf(TileSpanSize(2, 2))
        }

        val available = gridConfig.availableSpecialSizes
        return candidates.firstOrNull { candidate ->
            available.any { it.spanWidth == candidate.spanWidth && it.spanHeight == candidate.spanHeight }
        } ?: TileSpanSize(1, 1)
    }

    fun uploadImages(files: List<PlatformFile>) {
        if (files.isEmpty()) return

        viewModelScope.launch {
            uploadMapAreaImagesUseCase(files, mapId, geoAreaId).collect { progress ->
                when (progress) {
                    is UploadProgress.Uploading -> {
                        _uploadState.value = UploadState.Uploading(
                            totalCount = progress.totalCount,
                            completedCount = progress.completedCount,
                        )
                    }

                    is UploadProgress.Completed -> {
                        refreshGrid()

                        progress.singleImageResult?.let { result ->
                            _navigateToPhotoDetail.emit(
                                PhotoDetailNavigation(result.imageId, result.imageUrl),
                            )
                        }

                        _uploadState.value = UploadState.Idle
                    }
                }
            }
        }
    }

    private suspend fun refreshGrid() {
        val latestRegions = mapRegionRepository.getMapRegionsByMapIdAndGeoAreaId(mapId, geoAreaId)
        val placementResult = buildPlacedItems(latestRegions)
        val currentState = _screenState.value
        if (currentState is ScreenState.Idle) {
            _screenState.value = ScreenState.Idle(
                currentState.data.copy(
                    mapRegions = latestRegions.toImmutableList(),
                    placedItems = placementResult.placedItems.toImmutableList(),
                    rowCount = placementResult.rowCount,
                ),
            )
        }
    }

    fun onItemLongClick(itemId: String) {
        _selectionState.value = SelectionState(
            isSelectionMode = true,
            selectedIds = persistentSetOf(itemId),
        )
    }

    fun onItemClickInSelectionMode(itemId: String) {
        val currentState = _selectionState.value
        if (!currentState.isSelectionMode) return

        val newSelectedIds = if (currentState.selectedIds.contains(itemId)) {
            (currentState.selectedIds - itemId).toImmutableSet()
        } else {
            (currentState.selectedIds + itemId).toImmutableSet()
        }

        if (newSelectedIds.isEmpty()) {
            _selectionState.value = SelectionState()
        } else {
            _selectionState.value = currentState.copy(selectedIds = newSelectedIds)
        }
    }

    fun exitSelectionMode() {
        _selectionState.value = SelectionState()
    }

    fun requestDelete() {
        val selectedCount = _selectionState.value.selectedIds.size
        if (selectedCount > 0) {
            _deleteState.value = DeleteState.Confirming(selectedCount)
        }
    }

    fun dismissDeleteDialog() {
        _deleteState.value = DeleteState.Idle
    }

    fun confirmDelete() {
        val selectedIds = _selectionState.value.selectedIds.toList()
        if (selectedIds.isEmpty()) {
            _deleteState.value = DeleteState.Idle
            return
        }

        viewModelScope.launch {
            deleteMapAreaImagesUseCase(selectedIds).collect { progress ->
                when (progress) {
                    is DeleteProgress.Deleting -> {
                        _deleteState.value = DeleteState.Deleting(
                            totalCount = progress.totalCount,
                            completedCount = progress.completedCount,
                        )
                    }

                    is DeleteProgress.Completed -> {
                        _deleteState.value = DeleteState.Idle
                        _selectionState.value = SelectionState()
                        refreshGrid()
                    }
                }
            }
        }
    }
}

@Stable
sealed interface UploadState {
    data object Idle : UploadState

    data class Uploading(
        val totalCount: Int,
        val completedCount: Int,
    ) : UploadState
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

@Stable
data class SelectionState(
    val isSelectionMode: Boolean = false,
    val selectedIds: ImmutableSet<String> = persistentSetOf(),
)

@Stable
sealed interface DeleteState {
    data object Idle : DeleteState

    data class Confirming(val count: Int) : DeleteState

    data class Deleting(
        val totalCount: Int,
        val completedCount: Int,
    ) : DeleteState
}
