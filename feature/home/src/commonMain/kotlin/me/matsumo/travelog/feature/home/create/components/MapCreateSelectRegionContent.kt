package me.matsumo.travelog.feature.home.create.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.screen.ScreenState
import me.matsumo.travelog.feature.home.create.MapCreateSelectRegionUiState

@Composable
internal fun MapCreateSelectRegionContent(
    screenState: ScreenState<MapCreateSelectRegionUiState>,
    onRetryClicked: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
        retryAction = onRetryClicked,
    ) {

    }
}