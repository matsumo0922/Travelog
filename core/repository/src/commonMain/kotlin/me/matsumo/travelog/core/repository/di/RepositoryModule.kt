package me.matsumo.travelog.core.repository.di

import me.matsumo.travelog.core.datasource.SessionStatusProvider
import me.matsumo.travelog.core.repository.AppSettingRepository
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.ImageCommentRepository
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.MapRegionRepository
import me.matsumo.travelog.core.repository.MapRepository
import me.matsumo.travelog.core.repository.SessionRepository
import me.matsumo.travelog.core.repository.StorageRepository
import me.matsumo.travelog.core.repository.UserRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::AppSettingRepository)
    singleOf(::GeoAreaRepository)
    singleOf(::SessionRepository) bind SessionStatusProvider::class
    singleOf(::UserRepository)
    singleOf(::ImageRepository)
    singleOf(::ImageCommentRepository)
    singleOf(::StorageRepository)
    singleOf(::MapRepository)
    singleOf(::MapRegionRepository)
}
