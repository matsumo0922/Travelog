package me.matsumo.travelog.feature.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import me.matsumo.travelog.core.model.Destination

fun NavGraphBuilder.homeScreen() {
    composable<Destination.Home> {
        HomeScreen(
            modifier = Modifier.fillMaxSize(),
        )
    }
}
