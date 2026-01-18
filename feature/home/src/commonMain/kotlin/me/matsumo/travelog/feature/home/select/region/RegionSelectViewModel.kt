package me.matsumo.travelog.feature.home.select.region

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.geo.GeoRegionGroup
import me.matsumo.travelog.core.repository.GeoRegionRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState

class RegionSelectViewModel(
    private val selectedRegion: SupportedRegion,
    private val geoRegionRepository: GeoRegionRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<RegionSelectUiState>>(ScreenState.Loading())
    val screenState = _screenState.asStateFlow()

    init {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                RegionSelectUiState(
                    region = selectedRegion,
                    groups = geoRegionRepository.getGroupsByGroupCode(selectedRegion.code3)
                        .sortedBy { it.admISO }
                        .toImmutableList()
                )
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }
}

@Stable
data class RegionSelectUiState(
    val region: SupportedRegion,
    val groups: ImmutableList<GeoRegionGroup>,
)