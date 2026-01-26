package me.matsumo.travelog.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import me.matsumo.travelog.core.model.db.Map
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.screen.Destination
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.feature.map.components.MapDetailCanvasSection
import me.matsumo.travelog.feature.map.components.MapDetailTopAppBar
import me.matsumo.travelog.feature.map.components.MapDetailTopSection
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun MapDetailRoute(
    mapId: String,
    modifier: Modifier = Modifier,
    viewModel: MapDetailViewModel = koinViewModel(
        key = mapId,
    ) {
        parametersOf(mapId)
    },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.fetch()
    }

    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
        retryAction = viewModel::fetch,
    ) {
        MapDetailScreen(
            modifier = Modifier.fillMaxSize(),
            map = it.map,
            geoArea = it.geoArea,
            regions = it.regions,
            regionImageUrls = it.regionImageUrls,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapDetailScreen(
    map: Map,
    geoArea: GeoArea,
    regions: ImmutableList<MapRegion>,
    regionImageUrls: ImmutableMap<String, String>,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MapDetailTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                scrollBehavior = scrollBehavior,
                onShareClicked = { },
                onSettingsClicked = {
                    map.id?.let { mapId ->
                        navBackStack.add(
                            Destination.MapSetting(
                                mapId = mapId,
                                map = map,
                                totalChildCount = geoArea.childCount,
                                regions = regions.toList(),
                            ),
                        )
                    }
                },
                onBackClicked = { navBackStack.removeLastOrNull() },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val currentMapId = map.id
                    val currentGeoAreaId = geoArea.id
                    if (currentMapId != null && currentGeoAreaId != null) {
                        navBackStack.add(
                            Destination.MapSelectRegion(
                                mapId = currentMapId,
                                geoAreaId = currentGeoAreaId,
                                regions = regions.toList(),
                                regionImageUrls = regionImageUrls.toMap(),
                            ),
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = paddingValues,
        ) {
            item {
                MapDetailTopSection(
                    modifier = Modifier.fillMaxWidth(),
                    map = map,
                    area = geoArea,
                    regions = regions,
                    onPhotosClicked = {},
                )
            }

            item {
                MapDetailCanvasSection(
                    modifier = Modifier.fillMaxWidth(),
                    geoArea = geoArea,
                    regions = regions,
                    regionImageUrls = regionImageUrls,
                )
            }
        }
    }
}
