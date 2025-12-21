package me.matsumo.travelog.core.datasource.di

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import me.matsumo.travelog.core.common.formatter
import me.matsumo.travelog.core.datasource.AppSettingDataSource
import me.matsumo.travelog.core.datasource.GeoBoundaryDataSource
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import io.ktor.client.plugins.logging.LogLevel as KtorLogLevel
import io.ktor.client.plugins.logging.Logger as KtorLogger

val dataSourceModule = module {
    single {
        HttpClient {
            install(Logging) {
                level = KtorLogLevel.INFO
                logger = object : KtorLogger {
                    override fun log(message: String) {
                        Napier.d(message)
                    }
                }
            }

            install(ContentNegotiation) {
                json(formatter)
            }

            install(HttpCache)
        }
    }

    singleOf(::AppSettingDataSource)
    singleOf(::GeoBoundaryDataSource)

    includes(dataSourcePlatformModule)
}

internal expect val dataSourcePlatformModule: Module
