package me.matsumo.travelog.feature.home

import kotlinx.serialization.Serializable

@Serializable
sealed interface HomeRoute {
    @Serializable
    data object Map : HomeRoute

    @Serializable
    data object Photos : HomeRoute
}