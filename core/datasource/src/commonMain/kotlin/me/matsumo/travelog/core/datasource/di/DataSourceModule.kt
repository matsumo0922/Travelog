package me.matsumo.travelog.core.datasource.di

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.appleNativeLogin
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
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
import me.matsumo.travelog.core.datasource.api.ImageApi
import me.matsumo.travelog.core.datasource.api.ImageCommentApi
import me.matsumo.travelog.core.datasource.api.MapApi
import me.matsumo.travelog.core.datasource.api.MapRegionApi
import me.matsumo.travelog.core.datasource.api.UserApi
import me.matsumo.travelog.core.model.AppConfig
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

            install(HttpTimeout) {
                requestTimeoutMillis = 30.seconds.inWholeMilliseconds
                connectTimeoutMillis = 30.seconds.inWholeMilliseconds
                socketTimeoutMillis = 30.seconds.inWholeMilliseconds
            }

            install(HttpCache)
        }
    }

    single {
        val appConfig = get<AppConfig>()

        createSupabaseClient(
            supabaseUrl = appConfig.supabaseUrl,
            supabaseKey = appConfig.supabaseKey,
        ) {
            defaultLogLevel = LogLevel.DEBUG
            defaultSerializer = KotlinXSerializer(formatter)

            install(Postgrest)
            install(Realtime)
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = "https"
                host = "travelog.dev"
            }
            install(ComposeAuth) {
                googleNativeLogin(appConfig.googleClientId)
                appleNativeLogin()
            }
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

    singleOf(::AppSettingDataSource)
    singleOf(::GeoBoundaryDataSource)
    singleOf(::OverpassDataSource)
    singleOf(::NominatimDataSource)
    singleOf(::WikipediaDataSource)

    includes(dataSourcePlatformModule)
}

internal expect val dataSourcePlatformModule: Module
