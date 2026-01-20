package me.matsumo.travelog.feature.home.create.metadata

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.geo.GeoRegionGroup
import me.matsumo.travelog.core.repository.GeoRegionRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState

class MapCreateViewModel(
    private val selectedRegion: SupportedRegion,
    private val selectedGroupAdmId: String?,
    private val geoRegionRepository: GeoRegionRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapCreateUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<MapCreateUiState>> = _screenState

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                val group = selectedGroupAdmId?.let { geoRegionRepository.getRegionGroupByAdmId(it) }

                MapCreateUiState(
                    region = selectedRegion,
                    group = group,
                )
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }
}

@Stable
data class MapCreateUiState(
    val region: SupportedRegion,
    val group: GeoRegionGroup?,
)
