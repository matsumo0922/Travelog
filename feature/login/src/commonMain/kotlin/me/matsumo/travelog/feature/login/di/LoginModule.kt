package me.matsumo.travelog.feature.login.di

import me.matsumo.travelog.feature.login.LoginViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val loginModule = module {
    viewModelOf(::LoginViewModel)
}
