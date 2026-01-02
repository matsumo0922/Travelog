package me.matsumo.travelog.feature.home.create

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.geo.OverpassResult
import me.matsumo.travelog.core.repository.GeoBoundaryRepository
import me.matsumo.travelog.core.repository.GeoRegionRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_download
import me.matsumo.travelog.core.resource.home_map_select_region_loading_message
import me.matsumo.travelog.core.ui.screen.ScreenState

class MapCreateViewModel(
    private val geoRegionRepository: GeoRegionRepository,
    private val geoBoundaryRepository: GeoBoundaryRepository,
) : ViewModel() {

    val selectRegionScreenState: StateFlow<ScreenState<MapCreateSelectRegionUiState>>
        field : MutableStateFlow<ScreenState<MapCreateSelectRegionUiState>> = MutableStateFlow(ScreenState.Loading())

    fun downloadRegion(region: SupportedRegion) {
        viewModelScope.launch {
            selectRegionScreenState.value = ScreenState.Loading(Res.string.home_map_select_region_loading_message)
            selectRegionScreenState.value = suspendRunCatching {
                MapCreateSelectRegionUiState(
                    region = region,
                    elements = geoBoundaryRepository.getAdmins(region.code).toImmutableList()
                )
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_download) }
            )
        }
    }
}

@Stable
data class MapCreateSelectRegionUiState(
    val region: SupportedRegion,
    val elements: ImmutableList<OverpassResult.Element>
)