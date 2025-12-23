package me.matsumo.travelog.feature.home.maps

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.matsumo.travelog.core.ui.screen.Destination
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.feature.home.maps.components.HomeMapsTopAppBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeMapsScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeMapsViewModel = koinViewModel(),
) {
    val navBackStack = LocalNavBackStack.current

    Scaffold(
        modifier = modifier,
        topBar = {
            HomeMapsTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                onSettingClicked = { navBackStack.add(Destination.Setting.Root) }
            )
        }
    ) {

    }
}