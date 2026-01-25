package me.matsumo.travelog.core.usecase.di

import me.matsumo.travelog.core.usecase.CreateMapUseCase
import me.matsumo.travelog.core.usecase.UploadMapIconUseCase
import me.matsumo.travelog.core.usecase.UploadMapRegionImageUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

expect val tempFileStorageModule: org.koin.core.module.Module

val useCaseModule = module {
    singleOf(::UploadMapIconUseCase)
    singleOf(::UploadMapRegionImageUseCase)
    singleOf(::CreateMapUseCase)
}
