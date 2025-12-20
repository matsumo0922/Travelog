package me.matsumo.travelog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import me.matsumo.travelog.core.model.Destination
import me.matsumo.travelog.core.ui.theme.LocalNavController
import me.matsumo.travelog.feature.home.homeScreen
import me.matsumo.travelog.feature.setting.oss.settingLicenseScreen
import me.matsumo.travelog.feature.setting.settingScreen

@Composable
internal fun AppNavHost(
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Destination.Home,
    ) {
        homeScreen()
        settingScreen()
        settingLicenseScreen()
    }
}
