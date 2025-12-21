package me.matsumo.travelog.di

import me.matsumo.travelog.core.common.di.commonModule
import me.matsumo.travelog.core.datasource.di.dataSourceModule
import me.matsumo.travelog.core.repository.di.repositoryModule
import me.matsumo.travelog.feature.home.di.homeModule
import me.matsumo.travelog.feature.login.di.loginModule
import me.matsumo.travelog.feature.setting.di.settingModule
import org.koin.core.KoinApplication

fun KoinApplication.applyModules() {
    modules(appModule)

    modules(commonModule)
    modules(dataSourceModule)
    modules(repositoryModule)

    modules(homeModule)
    modules(settingModule)
    modules(loginModule)
}
