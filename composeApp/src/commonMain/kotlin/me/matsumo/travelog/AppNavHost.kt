package me.matsumo.travelog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import me.matsumo.travelog.core.ui.animation.NavigationTransitions
import me.matsumo.travelog.core.ui.screen.Destination
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.feature.home.homeEntry
import me.matsumo.travelog.feature.login.loginEntry
import me.matsumo.travelog.feature.setting.oss.settingLicenseEntry
import me.matsumo.travelog.feature.setting.settingEntry

@Composable
internal fun AppNavHost(
    modifier: Modifier = Modifier,
    initialDestination: Destination = Destination.Home,
) {
    val navBackStack = rememberNavBackStack(Destination.config, initialDestination)

    CompositionLocalProvider(
        LocalNavBackStack provides navBackStack,
    ) {
        NavDisplay(
            modifier = modifier,
            backStack = navBackStack,
            entryProvider = entryProvider {
                homeEntry()
                loginEntry()
                settingEntry()
                settingLicenseEntry()
            },
            transitionSpec = { NavigationTransitions.forwardTransition },
            popTransitionSpec = { NavigationTransitions.backwardTransition },
            predictivePopTransitionSpec = { NavigationTransitions.backwardTransition },
        )
    }
}
