package me.matsumo.travelog.feature.login

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.matsumo.travelog.core.model.Destination
import me.matsumo.travelog.core.ui.screen.Destination2

fun NavGraphBuilder.loginScreen() {
    composable<Destination.Login> {
        LoginRoute(
            modifier = Modifier.fillMaxSize(),
        )
    }
}

fun EntryProviderScope<NavKey>.loginEntry() {
    entry<Destination2.Login> {
        LoginRoute(
            modifier = Modifier.fillMaxSize(),
        )
    }
}
