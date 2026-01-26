package me.matsumo.travelog.feature.map.area

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.MapRegionRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.component.PlacedTileItem
import me.matsumo.travelog.core.ui.component.TileGridConfig
import me.matsumo.travelog.core.ui.component.TileGridPlacer
import me.matsumo.travelog.core.ui.screen.ScreenState
import me.matsumo.travelog.core.usecase.GetMapRegionImagesUseCase
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
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapAreaDetailUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<MapAreaDetailUiState>> = _screenState.asStateFlow()

    private val gridConfig = TileGridConfig()

    init {
        fetch()
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
}

@Stable
data class MapAreaDetailUiState(
    val geoArea: GeoArea,
    val mapRegions: ImmutableList<MapRegion>,
    val regionImageUrls: ImmutableMap<String, String> = persistentMapOf(),
    val placedItems: ImmutableList<PlacedTileItem<GridPhotoItem>> = persistentListOf(),
    val rowCount: Int = 0,
)
