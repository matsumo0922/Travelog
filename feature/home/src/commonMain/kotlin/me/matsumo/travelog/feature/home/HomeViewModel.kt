package me.matsumo.travelog.feature.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.model.GeoBoundaryLevel
import me.matsumo.travelog.core.model.GeoJsonData
import me.matsumo.travelog.core.repository.GeoBoundaryRepository

internal class HomeViewModel(
    private val geoBoundaryRepository: GeoBoundaryRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState>
        field : MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState(null))

    init {
        viewModelScope.launch {
            val country = geoBoundaryRepository.getBoundaryInfo("JPN", GeoBoundaryLevel.ADM1)
            val data = geoBoundaryRepository.downloadGeoJsonFromUrl(country.simplifiedGeometryGeoJSON!!)

            uiState.value = HomeUiState(data)
        }
    }
}

@Stable
internal data class HomeUiState(
    val geoJsonData: GeoJsonData?,
)