package me.matsumo.travelog.feature.home.select

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.model.Region
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.repository.GeoRegionRepository
import me.matsumo.travelog.core.ui.screen.ScreenState

class CountrySelectViewModel(
    private val geoRegionRepository: GeoRegionRepository,
) : ViewModel() {
    private val _screenState = MutableStateFlow<ScreenState<CountrySelectUiState>>(ScreenState.Loading())
    val screenState = _screenState.asStateFlow()

    init {
        viewModelScope.launch {
            val supportedRegions = SupportedRegion.all
            val availableCountryCodes = geoRegionRepository.getAvailableCountryCodes()
            val availableRegions = supportedRegions.filter { it.code2 in availableCountryCodes }
            val groups = availableRegions.map {
                async {
                    Region(
                        supportedRegion = it,
                        groups = geoRegionRepository.getGroupsByGroupCode(it.code2)
                    )
                }
            }.awaitAll()

            _screenState.value = ScreenState.Idle(
                CountrySelectUiState(
                    regions = groups,
                ),
            )
        }
    }
}

@Stable
data class CountrySelectUiState(
    val regions: List<Region>,
)
