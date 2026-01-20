package me.matsumo.travelog.feature.home.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.model.geo.GeoAreaLevel
import me.matsumo.travelog.core.repository.GeoAreaRepository

class HomePhotosViewModel(
    private val geoAreaRepository: GeoAreaRepository,
) : ViewModel() {

    val areas = MutableStateFlow<List<GeoArea>>(emptyList())

    init {
        viewModelScope.launch {
            areas.value = suspendRunCatching {
                // Get ADM1 areas for Japan
                val adm1Areas = geoAreaRepository.getAreasByLevel("JP", GeoAreaLevel.ADM1)
                // Find Kagoshima prefecture
                val kagoshima = adm1Areas.find { it.nameJa?.contains("鹿児島") == true }
                    ?: return@suspendRunCatching emptyList()
                // Get children (ADM2 areas)
                kagoshima.id?.let { geoAreaRepository.getChildren(it) } ?: emptyList()
            }.onSuccess {
                val names = it.map { area -> area.name }
                Napier.d(tag = "GeoBoundary") { "Fetched areas=${names.joinToString()}" }
            }.onFailure {
                Napier.e(it) { "Failed to fetch areas" }
            }.getOrElse { emptyList() }
        }
    }
}
