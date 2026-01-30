package me.matsumo.travelog.feature.map.area

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import me.matsumo.travelog.feature.map.area.components.DeleteConfirmDialog
import me.matsumo.travelog.feature.map.area.components.DeleteProgressDialog
import me.matsumo.travelog.feature.map.area.components.MapAreaDetailFab
import me.matsumo.travelog.feature.map.area.components.MapAreaDetailHeader
import me.matsumo.travelog.feature.map.area.components.MapAreaDetailTopAppBar
import me.matsumo.travelog.feature.map.area.components.SelectablePhotoItem
import me.matsumo.travelog.feature.map.area.components.UploadProgressDialog
import me.matsumo.travelog.feature.map.area.components.model.GridPhotoItem
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun MapAreaDetailRoute(
    mapId: String,
    geoAreaId: String,
    initialRegions: ImmutableList<MapRegion>?,
    initialRegionImageUrls: ImmutableMap<String, String>?,
    modifier: Modifier = Modifier,
    tempFileStorage: TempFileStorage = koinInject(),
    viewModel: MapAreaDetailViewModel = koinViewModel(
        key = "$mapId-$geoAreaId",
    ) {
        parametersOf(mapId, geoAreaId, initialRegions, initialRegionImageUrls)
    },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val selectionState by viewModel.selectionState.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()
    val navBackStack = LocalNavBackStack.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.fetch()
    }

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
            onImagesPicked = viewModel::uploadImages,
            uploadState = uploadState,
            selectionState = selectionState,
            deleteState = deleteState,
            onItemLongClick = viewModel::onItemLongClick,
            onItemClickInSelectionMode = viewModel::onItemClickInSelectionMode,
            onExitSelectionMode = viewModel::exitSelectionMode,
            onRequestDelete = viewModel::requestDelete,
            onConfirmDelete = viewModel::confirmDelete,
            onDismissDeleteDialog = viewModel::dismissDeleteDialog,
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
    onImagesPicked: (List<PlatformFile>) -> Unit,
    uploadState: UploadState,
    selectionState: SelectionState,
    deleteState: DeleteState,
    onItemLongClick: (String) -> Unit,
    onItemClickInSelectionMode: (String) -> Unit,
    onExitSelectionMode: () -> Unit,
    onRequestDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUploading = uploadState is UploadState.Uploading
    val isDeleting = deleteState is DeleteState.Deleting
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
                isSelectionMode = selectionState.isSelectionMode,
                selectedCount = selectionState.selectedIds.size,
                onBackClicked = { navBackStack.removeLastOrNull() },
                onDeleteClicked = onRequestDelete,
                onCloseSelectionMode = onExitSelectionMode,
            )
        },
        floatingActionButton = {
            if (!isUploading && !isDeleting && !selectionState.isSelectionMode) {
                MapAreaDetailFab(
                    onImagesPicked = onImagesPicked,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        TileGrid(
            modifier = Modifier.fillMaxSize(),
            placedItems = placedItems,
            rowCount = rowCount,
            columnCount = 3,
            cornerRadius = 16.dp,
            cellSpacing = 6.dp,
            contentPadding = paddingValues + PaddingValues(8.dp),
            isSelectionMode = selectionState.isSelectionMode,
            selectedIds = selectionState.selectedIds,
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
                if (selectionState.isSelectionMode) {
                    onItemClickInSelectionMode(item.id)
                } else {
                    navBackStack.add(
                        Destination.PhotoDetail(
                            imageId = item.id,
                            imageUrl = item.imageUrl,
                            regionName = geoArea.nameJa ?: geoArea.name,
                        ),
                    )
                }
            },
            onItemLongClick = { item ->
                onItemLongClick(item.id)
            },
        ) { item, isSelected ->
            SelectablePhotoItem(
                imageUrl = item.imageUrl,
                isSelectionMode = selectionState.isSelectionMode,
                isSelected = isSelected,
            )
        }
    }

    UploadProgressDialog(uploadState = uploadState)

    when (val state = deleteState) {
        is DeleteState.Confirming -> {
            DeleteConfirmDialog(
                count = state.count,
                onConfirm = onConfirmDelete,
                onDismiss = onDismissDeleteDialog,
            )
        }

        is DeleteState.Deleting -> {
            DeleteProgressDialog(deleteState = state)
        }

        DeleteState.Idle -> {}
    }
}
