package me.matsumo.travelog.core.usecase.di

import me.matsumo.travelog.core.usecase.UploadMapIconUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val useCaseModule = module {
    singleOf(::UploadMapIconUseCase)
}
