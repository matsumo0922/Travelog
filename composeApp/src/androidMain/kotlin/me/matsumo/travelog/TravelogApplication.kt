package me.matsumo.travelog

import android.app.Application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import me.matsumo.travelog.di.applyModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

@OptIn(KoinExperimentalAPI::class)
class TravelogApplication : Application(), KoinStartup {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            // StrictMode.enableDefaults()
            Napier.base(DebugAntilog())
        }
    }

    override fun onKoinStartup() = koinConfiguration {
        androidContext(this@TravelogApplication)
        androidLogger()
        applyModules()
    }
}
