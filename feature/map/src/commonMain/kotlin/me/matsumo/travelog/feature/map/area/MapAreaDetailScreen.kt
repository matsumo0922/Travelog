package me.matsumo.travelog.feature.map.area

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.ui.component.PlacedTileItem
import me.matsumo.travelog.core.ui.component.TileGrid
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.screen.Destination
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.core.ui.utils.plus
import me.matsumo.travelog.core.usecase.TempFileStorage
import me.matsumo.travelog.feature.map.area.components.MapAreaDetailFab
import me.matsumo.travelog.feature.map.area.components.MapAreaDetailHeader
import me.matsumo.travelog.feature.map.area.components.MapAreaDetailTopAppBar
import me.matsumo.travelog.feature.map.area.components.TilePhotoItem
import me.matsumo.travelog.feature.map.area.components.model.GridPhotoItem
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun MapAreaDetailRoute(
    mapId: String,
    geoAreaId: String,
    initialRegions: List<MapRegion>?,
    initialRegionImageUrls: Map<String, String>?,
    modifier: Modifier = Modifier,
    tempFileStorage: TempFileStorage = koinInject(),
    viewModel: MapAreaDetailViewModel = koinViewModel(
        key = "$mapId-$geoAreaId",
    ) {
        parametersOf(mapId, geoAreaId, initialRegions, initialRegionImageUrls)
    },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()
    val navBackStack = LocalNavBackStack.current

    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
        retryAction = viewModel::fetch,
    ) {
        val regionNameState = rememberUpdatedState(it.geoArea.nameJa ?: it.geoArea.name)
        LaunchedEffect(viewModel) {
            viewModel.navigateToPhotoDetail.collect { event ->
                navBackStack.add(
                    Destination.PhotoDetail(
                        imageId = event.imageId,
                        imageUrl = event.imageUrl,
                        regionName = regionNameState.value,
                    ),
                )
            }
        }

        MapAreaDetailScreen(
            modifier = Modifier.fillMaxSize(),
            mapId = mapId,
            geoAreaId = geoAreaId,
            geoArea = it.geoArea,
            mapRegions = it.mapRegions,
            regionImageUrls = it.regionImageUrls,
            placedItems = it.placedItems,
            rowCount = it.rowCount,
            tempFileStorage = tempFileStorage,
            onImagePicked = viewModel::uploadImage,
            isUploading = isUploading,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapAreaDetailScreen(
    mapId: String,
    geoAreaId: String,
    geoArea: GeoArea,
    mapRegions: ImmutableList<MapRegion>,
    regionImageUrls: ImmutableMap<String, String>,
    placedItems: ImmutableList<PlacedTileItem<GridPhotoItem>>,
    rowCount: Int,
    tempFileStorage: TempFileStorage,
    onImagePicked: (PlatformFile) -> Unit,
    isUploading: Boolean,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Find existing region for this geoArea
    val existingRegion = mapRegions.firstOrNull()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MapAreaDetailTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                scrollBehavior = scrollBehavior,
                onBackClicked = { navBackStack.removeLastOrNull() },
            )
        },
        floatingActionButton = {
            MapAreaDetailFab(
                onImagePicked = onImagePicked,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            TileGrid(
                modifier = Modifier.fillMaxSize(),
                placedItems = placedItems,
                rowCount = rowCount,
                columnCount = 3,
                cornerRadius = 16.dp,
                cellSpacing = 6.dp,
                contentPadding = paddingValues + PaddingValues(8.dp),
                header = {
                    MapAreaDetailHeader(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .fillMaxWidth(),
                        mapId = mapId,
                        geoAreaId = geoAreaId,
                        geoArea = geoArea,
                        mapRegions = mapRegions,
                        regionImageUrls = regionImageUrls,
                        existingRegionId = existingRegion?.id,
                        tempFileStorage = tempFileStorage,
                    )
                },
                onItemClick = { item ->
                    navBackStack.add(
                        Destination.PhotoDetail(
                            imageId = item.id,
                            imageUrl = item.imageUrl,
                            regionName = geoArea.nameJa ?: geoArea.name,
                        ),
                    )
                },
            ) { item ->
                TilePhotoItem(imageUrl = item.imageUrl)
            }

            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
