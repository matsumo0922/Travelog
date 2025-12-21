package me.matsumo.travelog.core.repository.di

import me.matsumo.travelog.core.repository.AppSettingRepository
import me.matsumo.travelog.core.repository.GeoBoundaryRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::AppSettingRepository)
    singleOf(::GeoBoundaryRepository)
}
