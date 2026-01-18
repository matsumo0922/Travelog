package me.matsumo.travelog.feature.home.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.geo.GeoRegion
import me.matsumo.travelog.core.repository.GeoRegionRepository

class HomePhotosViewModel(
    private val geoRegionRepository: GeoRegionRepository,
) : ViewModel() {

    val regions = MutableStateFlow<List<GeoRegion>>(emptyList())

    init {
        viewModelScope.launch {
            regions.value = suspendRunCatching {
                val groups = geoRegionRepository.getGroupsByGroupCode("JPN")
                val group = groups.find { it.nameJa?.contains("鹿児島") == true } ?: return@suspendRunCatching emptyList()
                geoRegionRepository.getRegionsByGroupId(group.id!!)
            }.onSuccess {
                val names = it.map { region -> region.name }
                Napier.d(tag = "GeoBoundary") { "Fetched regions=${names.joinToString()}" }
            }.onFailure {
                Napier.e(it) { "Failed to fetch regions" }
            }.getOrElse { emptyList() }
        }
    }
}
