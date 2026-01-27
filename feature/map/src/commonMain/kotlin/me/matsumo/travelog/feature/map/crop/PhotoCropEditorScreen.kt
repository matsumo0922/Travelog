package me.matsumo.travelog.feature.map.crop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.photo_crop_editor_save_error
import me.matsumo.travelog.core.resource.photo_crop_editor_saving
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.feature.map.crop.components.CropEditorCanvas
import me.matsumo.travelog.feature.map.crop.components.CropEditorControls
import me.matsumo.travelog.feature.map.crop.components.CropEditorTopAppBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun PhotoCropEditorRoute(
    mapId: String,
    geoAreaId: String,
    localFilePath: String,
    existingRegionId: String?,
    modifier: Modifier = Modifier,
    viewModel: PhotoCropEditorViewModel = koinViewModel(
        key = "$mapId-$geoAreaId-$localFilePath",
    ) {
        parametersOf(mapId, geoAreaId, localFilePath, existingRegionId)
    },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val navBackStack = LocalNavBackStack.current
    val snackbarHostState = remember { SnackbarHostState() }
    val saveErrorMessage = stringResource(Res.string.photo_crop_editor_save_error)

    LaunchedEffect(saveState) {
        if (saveState is SaveState.Error) {
            snackbarHostState.showSnackbar(saveErrorMessage)
        }
    }

    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
        retryAction = viewModel::fetch,
    ) { uiState ->
        PhotoCropEditorScreen(
            modifier = Modifier.fillMaxSize(),
            geoArea = uiState.geoArea,
            localFilePath = uiState.localFilePath,
            cropTransform = uiState.cropTransform,
            isSaving = saveState is SaveState.Uploading || saveState is SaveState.Saving,
            snackbarHostState = snackbarHostState,
            onTransformChanged = viewModel::updateTransform,
            onSaveClicked = {
                viewModel.save {
                    // Navigate back to MapDetail on success
                    navBackStack.removeLastOrNull()
                    navBackStack.removeLastOrNull() // Also remove MapAreaDetail
                }
            },
            onBackClicked = { navBackStack.removeLastOrNull() },
        )
    }
}

@Composable
private fun PhotoCropEditorScreen(
    geoArea: GeoArea,
    localFilePath: String,
    cropTransform: CropTransformState,
    isSaving: Boolean,
    snackbarHostState: SnackbarHostState,
    onTransformChanged: (scale: Float, offsetX: Float, offsetY: Float) -> Unit,
    onSaveClicked: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 保存中はローディングダイアログを表示
    if (isSaving) {
        PhotoCropSavingDialog()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CropEditorTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                onBackClicked = onBackClicked,
                onSaveClicked = onSaveClicked,
                isSaving = isSaving,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets(),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            CropEditorCanvas(
                modifier = Modifier.fillMaxSize(),
                localFilePath = localFilePath,
                geoArea = geoArea,
                initialTransform = cropTransform,
                onTransformChanged = onTransformChanged,
            )

            CropEditorControls(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                onZoomIn = {
                    val newScale = (cropTransform.scale * 1.2f).coerceIn(0.5f, 5f)
                    onTransformChanged(newScale, cropTransform.offsetX, cropTransform.offsetY)
                },
                onZoomOut = {
                    val newScale = (cropTransform.scale / 1.2f).coerceIn(0.5f, 5f)
                    onTransformChanged(newScale, cropTransform.offsetX, cropTransform.offsetY)
                },
                onReset = {
                    onTransformChanged(1f, 0f, 0f)
                },
            )
        }
    }
}

@Composable
private fun PhotoCropSavingDialog() {
    Dialog(onDismissRequest = { }) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text(
                    text = stringResource(Res.string.photo_crop_editor_saving),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
