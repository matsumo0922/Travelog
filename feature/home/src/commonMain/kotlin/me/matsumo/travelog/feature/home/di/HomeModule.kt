package me.matsumo.travelog.feature.home.di

import me.matsumo.travelog.feature.home.HomeViewModel
import me.matsumo.travelog.feature.home.maps.HomeMapsViewModel
import me.matsumo.travelog.feature.home.photos.HomePhotosViewModel
import me.matsumo.travelog.feature.home.select.CountrySelectViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::HomeMapsViewModel)
    viewModelOf(::HomePhotosViewModel)
    viewModelOf(::CountrySelectViewModel)
}
