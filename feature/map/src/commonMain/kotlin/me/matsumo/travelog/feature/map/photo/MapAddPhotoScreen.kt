package me.matsumo.travelog.feature.map.photo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.ui.component.PlacedTileItem
import me.matsumo.travelog.core.ui.component.TileGrid
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.core.ui.utils.getLocalizedName
import me.matsumo.travelog.feature.map.photo.components.MapAddPhotoFab
import me.matsumo.travelog.feature.map.photo.components.MapAddPhotoTopAppBar
import me.matsumo.travelog.feature.map.photo.components.TilePhotoItem
import me.matsumo.travelog.feature.map.photo.components.model.GridPhotoItem
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun MapAddPhotoRoute(
    mapId: String,
    geoAreaId: String,
    modifier: Modifier = Modifier,
    viewModel: MapAddPhotoViewModel = koinViewModel(
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
        MapAddPhotoScreen(
            modifier = Modifier.fillMaxSize(),
            geoArea = it.geoArea,
            mapRegions = it.mapRegions,
            placedItems = it.placedItems,
            rowCount = it.rowCount,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapAddPhotoScreen(
    geoArea: GeoArea,
    mapRegions: ImmutableList<MapRegion>,
    placedItems: ImmutableList<PlacedTileItem<GridPhotoItem>>,
    rowCount: Int,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MapAddPhotoTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                areaName = geoArea.getLocalizedName(),
                scrollBehavior = scrollBehavior,
                onBackClicked = { navBackStack.removeLastOrNull() },
            )
        },
        floatingActionButton = {
            MapAddPhotoFab()
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        TileGrid(
            modifier = Modifier.fillMaxSize(),
            placedItems = placedItems,
            rowCount = rowCount,
            contentPadding = paddingValues,
        ) { item ->
            TilePhotoItem(imageUrl = item.imageUrl)
        }
    }
}
