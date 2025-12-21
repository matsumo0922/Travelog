package me.matsumo.travelog.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.repository.GeoBoundaryRepository

internal class HomeViewModel(
    private val geoBoundaryRepository: GeoBoundaryRepository,
) : ViewModel() {
    init {
        viewModelScope.launch {
            val countries = geoBoundaryRepository.getAllCountries()
            Napier.d("countries: $countries")
        }
    }
}
