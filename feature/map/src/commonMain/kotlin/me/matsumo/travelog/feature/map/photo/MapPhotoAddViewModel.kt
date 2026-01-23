package me.matsumo.travelog.feature.map.photo

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.MapRegionRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState

class MapPhotoAddViewModel(
    private val mapId: String,
    private val geoAreaId: String,
    private val geoAreaRepository: GeoAreaRepository,
    private val mapRegionRepository: MapRegionRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapPhotoAddUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<MapPhotoAddUiState>> = _screenState.asStateFlow()

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = ScreenState.Loading()

            val geoArea = geoAreaRepository.getAreaByIdWithChildren(geoAreaId, useCache = true)
            if (geoArea == null) {
                _screenState.value = ScreenState.Error(Res.string.error_network)
                return@launch
            }

            val mapRegions = mapRegionRepository.getMapRegionsByMapId(mapId)
                .filter { it.geoAreaId == geoAreaId }

            _screenState.value = ScreenState.Idle(
                MapPhotoAddUiState(
                    geoArea = geoArea,
                    mapRegions = mapRegions.toImmutableList(),
                ),
            )
        }
    }
}

@Stable
data class MapPhotoAddUiState(
    val geoArea: GeoArea,
    val mapRegions: ImmutableList<MapRegion>,
)
