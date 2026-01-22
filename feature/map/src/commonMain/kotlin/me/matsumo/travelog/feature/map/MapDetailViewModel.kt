package me.matsumo.travelog.feature.map

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.db.Map
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.MapRegionRepository
import me.matsumo.travelog.core.repository.MapRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState

class MapDetailViewModel(
    private val mapId: String,
    private val mapRepository: MapRepository,
    private val mapRegionRepository: MapRegionRepository,
    private val geoAreaRepository: GeoAreaRepository,
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
                val regions = mapRegionRepository.getMapRegionsByMapId(mapId)

                val regionsWithAreas = regions.map { region ->
                    async {
                        val geoArea = geoAreaRepository.getAreaById(region.geoAreaId)
                        if (geoArea != null) {
                            MapRegionWithArea(region, geoArea)
                        } else {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()

                MapDetailUiState(
                    map = map,
                    regionsWithAreas = regionsWithAreas,
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
    val regionsWithAreas: List<MapRegionWithArea>,
)

@Stable
data class MapRegionWithArea(
    val region: MapRegion,
    val geoArea: GeoArea,
)
