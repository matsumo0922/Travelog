package me.matsumo.travelog.feature.home.di

import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.feature.home.HomeViewModel
import me.matsumo.travelog.feature.home.create.region.RegionSelectViewModel
import me.matsumo.travelog.feature.home.maps.HomeMapsViewModel
import me.matsumo.travelog.feature.home.photos.HomePhotosViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::HomeMapsViewModel)
    viewModelOf(::HomePhotosViewModel)

    viewModel { extras ->
        RegionSelectViewModel(
            selectedRegion = SupportedRegion.all.first { it.code3 == extras.get<String>() },
            geoRegionRepository = get(),
        )
    }
}
