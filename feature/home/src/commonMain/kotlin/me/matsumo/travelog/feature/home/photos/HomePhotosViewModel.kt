package me.matsumo.travelog.feature.home.photos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.geo.EnrichedRegion
import me.matsumo.travelog.core.repository.GeoBoundaryRepository

class HomePhotosViewModel(
    private val geoBoundaryRepository: GeoBoundaryRepository,
) : ViewModel() {

    val regions = MutableStateFlow<List<EnrichedRegion>>(emptyList())

    init {
        viewModelScope.launch {
            regions.value = suspendRunCatching {
                geoBoundaryRepository.getEnrichedAdmins("JP", "埼玉県")
            }.onSuccess {
                Napier.d(tag = "GeoBoundary") { "Fetched regions count=${it.size}" }
            }.onFailure {
                Napier.e(it) { "Failed to fetch regions" }
            }.getOrElse { emptyList() }
        }
    }
}