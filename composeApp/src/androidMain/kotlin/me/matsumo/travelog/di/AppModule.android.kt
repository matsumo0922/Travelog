package me.matsumo.travelog.di

import me.matsumo.travelog.MainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal actual val appModulePlatform: Module = module {
    viewModelOf(::MainViewModel)
}
