package me.matsumo.travelog.feature.home.create.metadata

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.model.geo.GeoAreaLevel
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState

class MapCreateViewModel(
    private val selectedRegion: SupportedRegion,
    private val selectedAreaAdmId: String?,
    private val geoAreaRepository: GeoAreaRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapCreateUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<MapCreateUiState>> = _screenState

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                // If selectedAreaAdmId is null, fetch country level (ADM0)
                // If selectedAreaAdmId is provided, fetch that specific area (ADM1)
                val selectedArea = if (selectedAreaAdmId != null) {
                    geoAreaRepository.getAreaByAdmId(selectedAreaAdmId, parentId = null)
                } else {
                    // Country level - fetch ADM0
                    geoAreaRepository.getAreasByLevel(selectedRegion.code2, GeoAreaLevel.ADM0)
                        .firstOrNull()
                }

                MapCreateUiState(
                    region = selectedRegion,
                    selectedArea = selectedArea,
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
    val selectedArea: GeoArea?,
)
