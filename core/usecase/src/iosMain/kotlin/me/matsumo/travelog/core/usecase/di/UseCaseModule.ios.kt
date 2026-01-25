package me.matsumo.travelog.core.usecase.di

import me.matsumo.travelog.core.usecase.IosTempFileStorage
import me.matsumo.travelog.core.usecase.TempFileStorage
import org.koin.dsl.module

actual val tempFileStorageModule = module {
    single<TempFileStorage> { IosTempFileStorage() }
}
