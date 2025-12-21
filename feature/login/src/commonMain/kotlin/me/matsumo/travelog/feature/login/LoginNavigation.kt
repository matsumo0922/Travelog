package me.matsumo.travelog.feature.login

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import me.matsumo.travelog.core.model.Destination

fun NavGraphBuilder.loginScreen() {
    composable<Destination.Login> {
        LoginScreen(
            modifier = Modifier.fillMaxSize(),
        )
    }
}
