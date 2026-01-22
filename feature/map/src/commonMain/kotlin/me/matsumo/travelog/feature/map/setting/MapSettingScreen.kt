package me.matsumo.travelog.feature.map.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.PlatformFile
import me.matsumo.travelog.core.model.db.Map
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.feature.map.setting.components.MapSettingConfirmDialog
import me.matsumo.travelog.feature.map.setting.components.MapSettingDetailsSection
import me.matsumo.travelog.feature.map.setting.components.MapSettingLoadingDialog
import me.matsumo.travelog.feature.map.setting.components.MapSettingMetadataSection
import me.matsumo.travelog.feature.map.setting.components.MapSettingStatisticsSection
import me.matsumo.travelog.feature.map.setting.components.MapSettingTextEditDialog
import me.matsumo.travelog.feature.map.setting.components.MapSettingTopAppBar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun MapSettingScreen(
    mapId: String,
    initialMap: Map?,
    initialGeoAreaId: String?,
    initialGeoAreaName: String?,
    initialTotalChildCount: Int?,
    initialRegions: List<MapRegion>?,
    modifier: Modifier = Modifier,
    viewModel: MapSettingViewModel = koinViewModel(
        key = mapId,
    ) {
        parametersOf(
            mapId,
            initialMap,
            initialGeoAreaId,
            initialGeoAreaName,
            initialTotalChildCount,
            initialRegions,
        )
    },
) {
    val navBackStack = LocalNavBackStack.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigateToHome.collect {
            while (navBackStack.size > 1) {
                navBackStack.removeLastOrNull()
            }
        }
    }

    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
        retryAction = viewModel::fetch,
    ) { uiState ->
        MapSettingIdleScreen(
            modifier = Modifier.fillMaxSize(),
            uiState = uiState,
            dialogState = dialogState,
            onBackClicked = { navBackStack.removeLastOrNull() },
            onTitleClicked = viewModel::showTitleEditDialog,
            onDescriptionClicked = viewModel::showDescriptionEditDialog,
            onIconFileChanged = viewModel::updateIconFile,
            onDeleteClicked = viewModel::showDeleteConfirmationDialog,
            onTitleConfirm = viewModel::updateTitle,
            onDescriptionConfirm = viewModel::updateDescription,
            onDeleteConfirm = viewModel::deleteMap,
            onDialogDismiss = viewModel::dismissDialog,
        )
    }
}

@Composable
private fun MapSettingIdleScreen(
    uiState: MapSettingUiState,
    dialogState: MapSettingDialogState,
    onBackClicked: () -> Unit,
    onTitleClicked: () -> Unit,
    onDescriptionClicked: () -> Unit,
    onIconFileChanged: (PlatformFile?) -> Unit,
    onDeleteClicked: () -> Unit,
    onTitleConfirm: (String) -> Unit,
    onDescriptionConfirm: (String) -> Unit,
    onDeleteConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HandleDialogs(
        dialogState = dialogState,
        onDismiss = onDialogDismiss,
        onTitleConfirm = onTitleConfirm,
        onDescriptionConfirm = onDescriptionConfirm,
        onDeleteConfirm = onDeleteConfirm,
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            MapSettingTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                onBackClicked = onBackClicked,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                MapSettingStatisticsSection(
                    modifier = Modifier.fillMaxWidth(),
                    geoArea = uiState.geoArea,
                    regionCount = uiState.regionCount,
                    totalChildCount = uiState.totalChildCount,
                    photoCount = 0,
                )
            }

            item {
                MapSettingMetadataSection(
                    modifier = Modifier.fillMaxWidth(),
                    map = uiState.map,
                    iconFile = uiState.iconFile,
                    onTitleClicked = onTitleClicked,
                    onDescriptionClicked = onDescriptionClicked,
                    onIconFileChanged = onIconFileChanged,
                )
            }

            item {
                MapSettingDetailsSection(
                    modifier = Modifier.fillMaxWidth(),
                    map = uiState.map,
                    geoAreaId = uiState.geoArea.id.orEmpty(),
                    onDeleteClicked = onDeleteClicked,
                )
            }
        }
    }
}

@Composable
private fun HandleDialogs(
    dialogState: MapSettingDialogState,
    onDismiss: () -> Unit,
    onTitleConfirm: (String) -> Unit,
    onDescriptionConfirm: (String) -> Unit,
    onDeleteConfirm: () -> Unit,
) {
    when (dialogState) {
        MapSettingDialogState.None -> Unit

        is MapSettingDialogState.TextEdit.Title -> {
            MapSettingTextEditDialog(
                titleType = MapSettingTextEditDialog.TitleType.Title,
                currentValue = dialogState.currentValue,
                onConfirm = onTitleConfirm,
                onDismiss = onDismiss,
            )
        }

        is MapSettingDialogState.TextEdit.Description -> {
            MapSettingTextEditDialog(
                titleType = MapSettingTextEditDialog.TitleType.Description,
                currentValue = dialogState.currentValue,
                onConfirm = onDescriptionConfirm,
                onDismiss = onDismiss,
            )
        }

        MapSettingDialogState.DeleteConfirmation -> {
            MapSettingConfirmDialog(
                onConfirm = onDeleteConfirm,
                onDismiss = onDismiss,
            )
        }

        is MapSettingDialogState.Loading -> {
            MapSettingLoadingDialog(loadingState = dialogState)
        }

        is MapSettingDialogState.Error -> {
            MapSettingConfirmDialog(
                isError = true,
                errorState = dialogState,
                onConfirm = onDismiss,
                onDismiss = onDismiss,
            )
        }
    }
}
