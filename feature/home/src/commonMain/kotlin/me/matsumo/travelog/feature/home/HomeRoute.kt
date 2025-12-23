package me.matsumo.travelog.feature.home

import kotlinx.serialization.Serializable

@Serializable
sealed interface HomeRoute {
    @Serializable
    data object Maps : HomeRoute

    @Serializable
    data object Photos : HomeRoute
}