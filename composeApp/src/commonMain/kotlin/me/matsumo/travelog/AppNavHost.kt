package me.matsumo.travelog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import me.matsumo.travelog.core.ui.screen.Destination
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.feature.home.homeEntry
import me.matsumo.travelog.feature.login.loginEntry
import me.matsumo.travelog.feature.setting.oss.settingLicenseEntry
import me.matsumo.travelog.feature.setting.settingEntry
import org.koin.compose.koinInject

@Composable
internal fun AppNavHost(
    modifier: Modifier = Modifier,
) {
    val navBackStack = rememberNavBackStack(Destination.config, Destination.Home)
    val supabaseClient = koinInject<SupabaseClient>()
    val sessionStatus by supabaseClient.auth.sessionStatus.collectAsStateWithLifecycle(null)

    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.NotAuthenticated && navBackStack.lastOrNull() != Destination.Login) {
            navBackStack.clear()
            navBackStack.addAll(listOf(Destination.Home, Destination.Login))
        }
    }

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
        )
    }
}
