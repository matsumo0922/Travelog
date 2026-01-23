package me.matsumo.travelog.feature.map.photo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.feature.map.photo.components.MapPhotoAddTopAppBar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun MapPhotoAddRoute(
    mapId: String,
    geoAreaId: String,
    modifier: Modifier = Modifier,
    viewModel: MapPhotoAddViewModel = koinViewModel(
        key = "$mapId-$geoAreaId",
    ) {
        parametersOf(mapId, geoAreaId)
    },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
        retryAction = viewModel::fetch,
    ) {
        MapPhotoAddScreen(
            modifier = Modifier.fillMaxSize(),
            uiState = it,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapPhotoAddScreen(
    uiState: MapPhotoAddUiState,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MapPhotoAddTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                scrollBehavior = scrollBehavior,
                onBackClicked = { navBackStack.removeLastOrNull() },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "MapPhotoAddScreen\n${uiState.geoArea.getLocalizedName()}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
