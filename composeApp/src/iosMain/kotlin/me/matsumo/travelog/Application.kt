package me.matsumo.travelog

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.matsumo.travelog.core.ui.screen.Destination
import me.matsumo.travelog.core.ui.state.AppStartupState
import org.koin.compose.viewmodel.koinViewModel

@Suppress("FunctionNaming")
fun MainViewController() = ComposeUIViewController {
    val viewModel = koinViewModel<MainViewModel>()
    val startupState by viewModel.startupState.collectAsStateWithLifecycle()

    Crossfade(targetState = startupState) { state ->
        when (state) {
            is AppStartupState.Loading -> {
                // ローディング中
            }

            is AppStartupState.Ready -> {
                TravelogApp(
                    modifier = Modifier.fillMaxSize(),
                    setting = state.setting,
                    initialDestination = Destination.initialDestination(state.isAuthenticated),
                )
            }
        }
    }
}
