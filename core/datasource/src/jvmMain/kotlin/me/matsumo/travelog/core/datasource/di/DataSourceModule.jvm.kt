package me.matsumo.travelog.core.datasource.di

import me.matsumo.travelog.core.datasource.GeoBoundaryCacheDataSource
import me.matsumo.travelog.core.datasource.GeoBoundaryCacheDataSourceImpl
import me.matsumo.travelog.core.datasource.helper.PreferenceHelper
import me.matsumo.travelog.core.datasource.helper.PreferenceHelperImpl
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val dataSourcePlatformModule: Module = module {
    single<PreferenceHelper> {
        PreferenceHelperImpl(
            ioDispatcher = get(),
        )
    }

    single<GeoBoundaryCacheDataSource> {
        GeoBoundaryCacheDataSourceImpl(
            ioDispatcher = get(),
        )
    }
}