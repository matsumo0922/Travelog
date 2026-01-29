package me.matsumo.travelog.feature.map.photo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.matsumo.travelog.core.model.db.ImageComment
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.common_cancel
import me.matsumo.travelog.core.resource.common_delete
import me.matsumo.travelog.core.resource.common_loading
import me.matsumo.travelog.core.resource.common_unknown
import me.matsumo.travelog.core.resource.photo_detail_comment_save_error
import me.matsumo.travelog.core.resource.photo_detail_comment_update_error
import me.matsumo.travelog.core.resource.photo_detail_delete_message
import me.matsumo.travelog.core.resource.photo_detail_delete_title
import me.matsumo.travelog.core.ui.component.AsyncImageWithPlaceholder
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.core.ui.utils.plus
import me.matsumo.travelog.feature.map.photo.components.PhotoCommentEditDialog
import me.matsumo.travelog.feature.map.photo.components.PhotoDetailCommentSection
import me.matsumo.travelog.feature.map.photo.components.PhotoDetailMetadataSection
import me.matsumo.travelog.feature.map.photo.components.PhotoDetailTopAppBar
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun PhotoDetailRoute(
    imageId: String,
    imageUrl: String?,
    regionName: String?,
    modifier: Modifier = Modifier,
    viewModel: PhotoDetailViewModel = koinViewModel(
        key = imageId,
    ) {
        parametersOf(imageId, imageUrl)
    },
) {
    val navBackStack = LocalNavBackStack.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect {
            navBackStack.removeLastOrNull()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            val message = when (event) {
                PhotoDetailUiEvent.CommentSaveFailed -> getString(Res.string.photo_detail_comment_save_error)
                PhotoDetailUiEvent.CommentUpdateFailed -> getString(Res.string.photo_detail_comment_update_error)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
        retryAction = viewModel::fetch,
    ) { uiState ->
        PhotoDetailScreen(
            modifier = Modifier.fillMaxSize(),
            uiState = uiState,
            regionName = regionName,
            dialogState = dialogState,
            isSaving = isSaving,
            snackbarHostState = snackbarHostState,
            onBackClicked = { navBackStack.removeLastOrNull() },
            onDeleteClicked = viewModel::deleteImage,
            onSaveClicked = viewModel::savePendingEdits,
            onCommentClicked = viewModel::showCommentEditDialog,
            onCommentEdited = viewModel::upsertComment,
            onDialogDismiss = viewModel::dismissDialog,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoDetailScreen(
    uiState: PhotoDetailUiState,
    regionName: String?,
    dialogState: PhotoDetailDialogState,
    isSaving: Boolean,
    snackbarHostState: SnackbarHostState,
    onBackClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    onCommentClicked: (ImageComment?) -> Unit,
    onCommentEdited: (ImageComment?, String) -> Unit,
    onDialogDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val titleText = regionName ?: stringResource(Res.string.common_unknown)
    val (isDeleteDialogVisible, setDeleteDialogVisible) = remember { mutableStateOf(false) }

    HandleDialogs(
        dialogState = dialogState,
        onDialogDismiss = onDialogDismiss,
        onCommentEdited = onCommentEdited,
    )

    if (isSaving) {
        PhotoDetailLoadingDialog()
    }

    if (isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { setDeleteDialogVisible(false) },
            title = { Text(stringResource(Res.string.photo_detail_delete_title)) },
            text = { Text(stringResource(Res.string.photo_detail_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        setDeleteDialogVisible(false)
                        onDeleteClicked()
                    },
                ) {
                    Text(stringResource(Res.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { setDeleteDialogVisible(false) }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PhotoDetailTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = titleText,
                onBackClicked = onBackClicked,
                onDeleteClicked = { setDeleteDialogVisible(true) },
                onSaveClicked = onSaveClicked,
                isSaving = isSaving,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues + PaddingValues(
                top = 8.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AsyncImageWithPlaceholder(
                    modifier = Modifier.fillMaxWidth(),
                    model = uiState.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                )
            }

            item {
                PhotoDetailCommentSection(
                    modifier = Modifier.fillMaxWidth(),
                    comments = uiState.comments,
                    onCommentClicked = onCommentClicked,
                )
            }

            item {
                PhotoDetailMetadataSection(
                    modifier = Modifier.fillMaxWidth(),
                    image = uiState.image,
                )
            }
        }
    }
}

@Composable
private fun PhotoDetailLoadingDialog() {
    Dialog(onDismissRequest = { }) {
        Card(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text(
                    text = stringResource(Res.string.common_loading),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun HandleDialogs(
    dialogState: PhotoDetailDialogState,
    onDialogDismiss: () -> Unit,
    onCommentEdited: (ImageComment?, String) -> Unit,
) {
    when (dialogState) {
        PhotoDetailDialogState.None -> Unit

        is PhotoDetailDialogState.CommentEdit -> {
            PhotoCommentEditDialog(
                currentValue = dialogState.comment?.body.orEmpty(),
                onConfirm = { onCommentEdited(dialogState.comment, it) },
                onDismiss = onDialogDismiss,
            )
        }
    }
}
