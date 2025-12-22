package me.matsumo.travelog.core.ui.screen

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface Destination2 : NavKey {
    @Serializable
    data object Home : Destination2

    @Serializable
    data object Login : Destination2

    @Serializable
    sealed interface Setting : Destination2 {
        @Serializable
        data object Root : Setting

        @Serializable
        data object License : Setting
    }

    companion object {
        val config = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(Destination2::class) {
                    subclass(Home::class, Home.serializer())
                    subclass(Login::class, Login.serializer())
                    subclass(Setting.Root::class, Setting.Root.serializer())
                    subclass(Setting.License::class, Setting.License.serializer())
                }
            }
        }
    }
}