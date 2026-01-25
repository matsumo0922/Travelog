package me.matsumo.travelog.feature.map.photo

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
import me.matsumo.travelog.feature.map.photo.components.MockPhotoGenerator
import me.matsumo.travelog.feature.map.photo.components.model.GridPhotoItem

class MapAddPhotoViewModel(
    private val mapId: String,
    private val geoAreaId: String,
    private val geoAreaRepository: GeoAreaRepository,
    private val mapRegionRepository: MapRegionRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapAddPhotoUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<MapAddPhotoUiState>> = _screenState.asStateFlow()

    private val gridConfig = TileGridConfig()

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                val geoArea = geoAreaRepository.getAreaByIdWithChildren(geoAreaId)
                val mapRegions = mapRegionRepository.getMapRegionsByMapIdAndGeoAreaId(mapId, geoAreaId)

                val mockPhotos = MockPhotoGenerator.generateMockPhotos(
                    count = 500,
                    config = gridConfig,
                )

                val placementResult = withContext(Dispatchers.Default) {
                    val placer = TileGridPlacer(gridConfig)
                    placer.placeItems(mockPhotos)
                }

                MapAddPhotoUiState(
                    geoArea = geoArea!!,
                    mapRegions = mapRegions.toImmutableList(),
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
data class MapAddPhotoUiState(
    val geoArea: GeoArea,
    val mapRegions: ImmutableList<MapRegion>,
    val placedItems: ImmutableList<PlacedTileItem<GridPhotoItem>> = persistentListOf(),
    val rowCount: Int = 0,
)
