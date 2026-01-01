package me.matsumo.travelog.feature.home.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.geo.EnrichedRegion
import me.matsumo.travelog.core.repository.GeoBoundaryRepository
import me.matsumo.travelog.core.repository.GeoRegionRepository

class HomePhotosViewModel(
    private val geoBoundaryRepository: GeoBoundaryRepository,
    private val geoRegionRepository: GeoRegionRepository,
) : ViewModel() {

    val regions = MutableStateFlow<List<EnrichedRegion>>(emptyList())

    init {
        viewModelScope.launch {
            regions.value = suspendRunCatching {
                val country = geoBoundaryRepository.getEnrichedCountries("JP")
                val admins = geoBoundaryRepository.getEnrichedAllAdmins(country)

                admins.map { it.regions }.flatten()
            }.onSuccess {
                val names = it.map { region -> region.name }
                Napier.d(tag = "GeoBoundary") { "Fetched regions=${names.joinToString()}" }
            }.onFailure {
                Napier.e(it) { "Failed to fetch regions" }
            }.getOrElse { emptyList() }
        }
    }
}