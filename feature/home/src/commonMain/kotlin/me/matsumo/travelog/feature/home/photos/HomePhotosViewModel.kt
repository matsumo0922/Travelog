package me.matsumo.travelog.feature.home.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.datasource.helper.GeoRegionMapper
import me.matsumo.travelog.core.model.geo.EnrichedRegion
import me.matsumo.travelog.core.repository.GeoRegionRepository

class HomePhotosViewModel(
    private val geoRegionRepository: GeoRegionRepository,
    private val geoRegionMapper: GeoRegionMapper,
) : ViewModel() {

    val regions = MutableStateFlow<List<EnrichedRegion>>(emptyList())

    init {
        viewModelScope.launch {
            regions.value = suspendRunCatching {
                val groupsDTO = geoRegionRepository.getGroupsByGroupCode("JPN")
                val groupDTO = groupsDTO.find { it.nameJa?.contains("鹿児島") == true } ?: return@suspendRunCatching emptyList()
                val regionsDTO = geoRegionRepository.getRegionsByGroupId(groupDTO.id!!)

                geoRegionMapper.toDomain(groupDTO, regionsDTO).regions
            }.onSuccess {
                val names = it.map { region -> region.name }
                Napier.d(tag = "GeoBoundary") { "Fetched regions=${names.joinToString()}" }
            }.onFailure {
                Napier.e(it) { "Failed to fetch regions" }
            }.getOrElse { emptyList() }
        }
    }
}