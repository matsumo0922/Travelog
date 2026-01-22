package me.matsumo.travelog.feature.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toImmutableList
import me.matsumo.travelog.core.model.db.Map
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.ui.component.GeoCanvasMap
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.feature.map.components.MapDetailTopAppBar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun MapDetailScreen(
    mapId: String,
    modifier: Modifier = Modifier,
    viewModel: MapDetailViewModel = koinViewModel(
        key = mapId,
    ) {
        parametersOf(mapId)
    },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
        retryAction = viewModel::fetch,
    ) {
        IdleScreen(
            modifier = Modifier.fillMaxSize(),
            map = it.map,
            geoArea = it.geoArea,
        )
    }
}

@Composable
private fun IdleScreen(
    map: Map,
    geoArea: GeoArea,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current

    Scaffold(
        modifier = modifier,
        topBar = {
            MapDetailTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = map.title,
                onBackClick = { navBackStack.removeLastOrNull() },
            )
        },
    ) { paddingValues ->
        GeoCanvasMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            areas = geoArea.children.toImmutableList(),
            strokeColor = MaterialTheme.colorScheme.outline,
            strokeWidth = 0.5f,
            fillColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        )
    }
}
