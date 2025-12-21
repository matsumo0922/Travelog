package me.matsumo.travelog.core.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {
    @Serializable
    data object Home : Destination

    @Serializable
    data object Login : Destination

    @Serializable
    sealed interface Setting : Destination {
        @Serializable
        data object Root : Setting

        @Serializable
        data object License : Setting
    }
}
