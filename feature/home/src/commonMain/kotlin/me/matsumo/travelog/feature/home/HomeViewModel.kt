package me.matsumo.travelog.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.model.GeoBoundaryLevel
import me.matsumo.travelog.core.repository.GeoBoundaryRepository

internal class HomeViewModel(
    private val geoBoundaryRepository: GeoBoundaryRepository,
) : ViewModel() {
    init {
        viewModelScope.launch {
            val country = geoBoundaryRepository.getBoundaryInfo("JPN", GeoBoundaryLevel.ADM1)
            val data = geoBoundaryRepository.downloadGeoJsonFromUrl(country.simplifiedGeometryGeoJSON!!)
        }
    }
}