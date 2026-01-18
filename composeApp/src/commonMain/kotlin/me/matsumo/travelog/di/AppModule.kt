package me.matsumo.travelog.di

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.appleNativeLogin
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import me.matsumo.travelog.BuildKonfig
import me.matsumo.travelog.MainViewModel
import me.matsumo.travelog.core.common.formatter
import me.matsumo.travelog.core.model.AppConfig
import me.matsumo.travelog.core.model.Platform
import me.matsumo.travelog.core.model.currentPlatform
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::MainViewModel)

    single<CoroutineDispatcher> {
        Dispatchers.IO.limitedParallelism(24)
    }

    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>())
    }

    single {
        val adMobAppId: String
        val adMobBannerAdUnitId: String
        val adMobInterstitialAdUnitId: String

        when (currentPlatform) {
            Platform.Android -> {
                adMobAppId = BuildKonfig.ADMOB_ANDROID_APP_ID
                adMobBannerAdUnitId = BuildKonfig.ADMOB_ANDROID_BANNER_AD_UNIT_ID
                adMobInterstitialAdUnitId = BuildKonfig.ADMOB_ANDROID_INTERSTITIAL_AD_UNIT_ID
            }

            Platform.IOS -> {
                adMobAppId = BuildKonfig.ADMOB_IOS_APP_ID
                adMobBannerAdUnitId = BuildKonfig.ADMOB_IOS_BANNER_AD_UNIT_ID
                adMobInterstitialAdUnitId = BuildKonfig.ADMOB_IOS_INTERSTITIAL_AD_UNIT_ID
            }

            Platform.JVM -> {
                adMobAppId = ""
                adMobBannerAdUnitId = ""
                adMobInterstitialAdUnitId = ""
            }
        }

        AppConfig(
            versionName = BuildKonfig.VERSION_NAME,
            versionCode = BuildKonfig.VERSION_CODE.toInt(),
            developerPin = BuildKonfig.DEVELOPER_PIN,
            purchaseAndroidApiKey = BuildKonfig.PURCHASE_ANDROID_API_KEY.takeIf { it.isNotBlank() },
            purchaseIosApiKey = BuildKonfig.PURCHASE_IOS_API_KEY.takeIf { it.isNotBlank() },
            adMobAppId = adMobAppId,
            adMobBannerAdUnitId = adMobBannerAdUnitId,
            adMobInterstitialAdUnitId = adMobInterstitialAdUnitId,
            supabaseUrl = BuildKonfig.SUPABASE_URL,
            supabaseKey = BuildKonfig.SUPABASE_KEY,
            googleClientId = BuildKonfig.GOOGLE_CLIENT_ID,
        )
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

    includes(appModulePlatform)
}

internal expect val appModulePlatform: Module
