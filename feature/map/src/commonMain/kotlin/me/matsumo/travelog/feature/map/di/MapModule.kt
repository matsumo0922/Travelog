package me.matsumo.travelog.feature.map.di

import me.matsumo.travelog.feature.map.MapDetailViewModel
import me.matsumo.travelog.feature.map.setting.MapSettingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mapModule = module {
    viewModel { extras ->
        MapDetailViewModel(
            mapId = extras.get<String>(),
            mapRepository = get(),
            mapRegionRepository = get(),
            geoAreaRepository = get(),
        )
    }

    viewModel { extras ->
        MapSettingViewModel(
            mapId = extras.get<String>(),
            mapRepository = get(),
            mapRegionRepository = get(),
            geoAreaRepository = get(),
            sessionRepository = get(),
            uploadMapIconUseCase = get(),
        )
    }
}
