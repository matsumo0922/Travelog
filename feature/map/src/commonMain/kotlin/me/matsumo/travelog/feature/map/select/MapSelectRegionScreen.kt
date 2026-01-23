package me.matsumo.travelog.feature.map.select

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.screen.Destination
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.feature.map.select.components.MapSelectRegionItem
import me.matsumo.travelog.feature.map.select.components.MapSelectRegionTopAppBar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun MapSelectRegionRoute(
    mapId: String,
    geoAreaId: String,
    modifier: Modifier = Modifier,
    viewModel: MapSelectRegionViewModel = koinViewModel(
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
        MapSelectRegionScreen(
            modifier = Modifier.fillMaxSize(),
            mapId = it.mapId,
            sortedChildren = it.sortedChildren,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapSelectRegionScreen(
    mapId: String,
    sortedChildren: ImmutableList<GeoArea>,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MapSelectRegionTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                scrollBehavior = scrollBehavior,
                onBackClicked = { navBackStack.removeLastOrNull() },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets()
    ) { paddingValues ->
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = sortedChildren,
                key = { it.id ?: it.admId },
            ) { area ->
                MapSelectRegionItem(
                    area = area,
                    onClick = {
                        area.id?.let {
                            navBackStack.add(Destination.MapAddPhoto(mapId, it))
                        }
                    },
                )
            }
        }
    }
}
