package me.matsumo.travelog.core.ui.screen

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Immutable
@Serializable
sealed interface Destination : NavKey {
    @Serializable
    data object Home : Destination

    @Serializable
    data object CountrySelect : Destination

    @Serializable
    data class RegionSelect(
        val selectedCountryCode3: String,
    ) : Destination

    @Serializable
    data class MapCreate(
        val selectedCountryCode3: String,
        val selectedGroupAdmId: String? = null,
    ) : Destination

    @Serializable
    data object Login : Destination

    @Serializable
    data class MapDetail(
        val mapId: String,
    ) : Destination

    @Serializable
    sealed interface Setting : Destination {
        @Serializable
        data object Root : Setting

        @Serializable
        data object License : Setting
    }

    companion object {
        val config = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Home::class, Home.serializer())
                    subclass(CountrySelect::class, CountrySelect.serializer())
                    subclass(RegionSelect::class, RegionSelect.serializer())
                    subclass(MapCreate::class, MapCreate.serializer())
                    subclass(Login::class, Login.serializer())
                    subclass(MapDetail::class, MapDetail.serializer())
                    subclass(Setting.Root::class, Setting.Root.serializer())
                    subclass(Setting.License::class, Setting.License.serializer())
                }
            }
        }

        fun initialDestination(isAuthenticated: Boolean): Destination {
            return if (isAuthenticated) Home else Login
        }
    }
}
