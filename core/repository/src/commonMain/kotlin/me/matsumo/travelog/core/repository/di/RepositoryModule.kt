package me.matsumo.travelog.core.repository.di

import me.matsumo.travelog.core.repository.AppSettingRepository
import me.matsumo.travelog.core.repository.GeoBoundaryMapper
import me.matsumo.travelog.core.repository.GeoBoundaryRepository
import me.matsumo.travelog.core.repository.GeoRegionRepository
import me.matsumo.travelog.core.repository.ImageCommentRepository
import me.matsumo.travelog.core.repository.ImageRepository
import me.matsumo.travelog.core.repository.MapRegionRepository
import me.matsumo.travelog.core.repository.MapRepository
import me.matsumo.travelog.core.repository.SessionRepository
import me.matsumo.travelog.core.repository.UserRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::AppSettingRepository)
    singleOf(::GeoBoundaryMapper)
    singleOf(::GeoBoundaryRepository)
    singleOf(::GeoRegionRepository)
    singleOf(::SessionRepository)
    singleOf(::UserRepository)
    singleOf(::MapRepository)
    singleOf(::MapRegionRepository)
    singleOf(::ImageRepository)
    singleOf(::ImageCommentRepository)
}
