package me.matsumo.travelog.feature.home.create.metadata.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.common_cancel
import me.matsumo.travelog.core.resource.common_error
import me.matsumo.travelog.core.resource.common_ok
import me.matsumo.travelog.core.resource.common_retry
import me.matsumo.travelog.core.resource.error_title_required
import me.matsumo.travelog.core.resource.home_map_create_error
import me.matsumo.travelog.core.resource.home_map_creating
import me.matsumo.travelog.core.resource.home_map_uploading_image
import me.matsumo.travelog.feature.home.create.metadata.MapCreateDialogState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MapCreateDialog(
    dialogState: MapCreateDialogState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (dialogState) {
        MapCreateDialogState.None -> Unit
        is MapCreateDialogState.Loading -> {
            MapCreateLoadingDialog(
                state = dialogState,
                modifier = modifier,
            )
        }

        is MapCreateDialogState.Error -> {
            MapCreateErrorDialog(
                state = dialogState,
                onRetry = onRetry,
                onCancel = onCancel,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun MapCreateLoadingDialog(
    state: MapCreateDialogState.Loading,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = { }) {
        Card(
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                )

                Text(
                    text = when (state) {
                        MapCreateDialogState.Loading.UploadingImage -> stringResource(Res.string.home_map_uploading_image)
                        MapCreateDialogState.Loading.CreatingMap -> stringResource(Res.string.home_map_creating)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun MapCreateErrorDialog(
    state: MapCreateDialogState.Error,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onCancel,
        title = {
            Text(text = stringResource(Res.string.common_error))
        },
        text = {
            Text(
                text = when (state) {
                    MapCreateDialogState.Error.TitleRequired -> stringResource(Res.string.error_title_required)
                    MapCreateDialogState.Error.UploadFailed -> stringResource(Res.string.home_map_create_error)
                    MapCreateDialogState.Error.CreateFailed -> stringResource(Res.string.home_map_create_error)
                },
            )
        },
        confirmButton = {
            if (state != MapCreateDialogState.Error.TitleRequired) {
                TextButton(onClick = onRetry) {
                    Text(text = stringResource(Res.string.common_retry))
                }
            } else {
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(Res.string.common_ok))
                }
            }
        },
        dismissButton = {
            if (state != MapCreateDialogState.Error.TitleRequired) {
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(Res.string.common_cancel))
                }
            }
        },
    )
}
