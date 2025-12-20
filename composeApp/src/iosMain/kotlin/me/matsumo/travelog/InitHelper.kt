package me.matsumo.travelog

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import me.matsumo.travelog.di.applyModules
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        applyModules()
    }
}

fun initNapier() {
    Napier.base(DebugAntilog())
}
