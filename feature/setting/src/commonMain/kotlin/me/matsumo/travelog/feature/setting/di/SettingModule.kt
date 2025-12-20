package me.matsumo.travelog.feature.setting.di

import me.matsumo.travelog.feature.setting.SettingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingModule = module {
    viewModelOf(::SettingViewModel)
}
