package me.matsumo.travelog.core.datasource.di

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.compose.auth.composeAuth
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import me.matsumo.travelog.core.common.formatter
import me.matsumo.travelog.core.datasource.AppSettingDataSource
import me.matsumo.travelog.core.datasource.GeoBoundaryDataSource
import me.matsumo.travelog.core.datasource.NominatimDataSource
import me.matsumo.travelog.core.datasource.OverpassDataSource
import me.matsumo.travelog.core.datasource.WikipediaDataSource
import me.matsumo.travelog.core.datasource.api.GeoAreaApi
import me.matsumo.travelog.core.datasource.api.ImageApi
import me.matsumo.travelog.core.datasource.api.ImageCommentApi
import me.matsumo.travelog.core.datasource.api.MapApi
import me.matsumo.travelog.core.datasource.api.MapRegionApi
import me.matsumo.travelog.core.datasource.api.StorageApi
import me.matsumo.travelog.core.datasource.api.UserApi
import me.matsumo.travelog.core.datasource.helper.GeoAreaMapper
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.plugins.logging.LogLevel as KtorLogLevel
import io.ktor.client.plugins.logging.Logger as KtorLogger

val dataSourceModule = module {
    single {
        HttpClient {
            install(Logging) {
                level = KtorLogLevel.NONE
                logger = object : KtorLogger {
                    override fun log(message: String) {
                        Napier.d(message)
                    }
                }
            }

            install(ContentNegotiation) {
                json(formatter)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 60.seconds.inWholeMilliseconds
                connectTimeoutMillis = 10.seconds.inWholeMilliseconds
                socketTimeoutMillis = 60.seconds.inWholeMilliseconds
            }

            install(HttpCache)
        }
    }

    single {
        get<SupabaseClient>().composeAuth
    }

    single { UserApi(get()) }
    single { MapApi(get()) }
    single { MapRegionApi(get()) }
    single { ImageApi(get()) }
    single { ImageCommentApi(get()) }
    single { GeoAreaApi(get()) }
    single { StorageApi(get()) }

    singleOf(::AppSettingDataSource)
    singleOf(::GeoBoundaryDataSource)
    singleOf(::OverpassDataSource)
    singleOf(::NominatimDataSource)
    singleOf(::WikipediaDataSource)

    singleOf(::GeoAreaMapper)

    includes(dataSourcePlatformModule)
}

internal expect val dataSourcePlatformModule: Module
