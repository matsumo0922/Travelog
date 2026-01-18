package me.matsumo.travelog.feature.home.select

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.repository.GeoRegionRepository
import me.matsumo.travelog.core.ui.screen.ScreenState

class CountrySelectViewModel(
    private val geoRegionRepository: GeoRegionRepository,
) : ViewModel() {
    private val _screenState = MutableStateFlow<ScreenState<CountrySelectUiState>>(ScreenState.Loading())
    val screenState = _screenState.asStateFlow()

    init {
        viewModelScope.launch {
            _screenState.value = ScreenState.Idle(
                CountrySelectUiState(
                    countryCodes = geoRegionRepository.getAvailableCountryCodes(),
                ),
            )
        }
    }
}

@Stable
data class CountrySelectUiState(
    val countryCodes: List<String>,
)
