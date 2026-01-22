package me.matsumo.travelog.feature.map.setting.components

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
import me.matsumo.travelog.core.resource.common_delete
import me.matsumo.travelog.core.resource.common_error
import me.matsumo.travelog.core.resource.common_ok
import me.matsumo.travelog.core.resource.map_setting_delete_message
import me.matsumo.travelog.core.resource.map_setting_delete_title
import me.matsumo.travelog.core.resource.map_setting_deleting
import me.matsumo.travelog.core.resource.map_setting_error_delete
import me.matsumo.travelog.core.resource.map_setting_error_update
import me.matsumo.travelog.core.resource.map_setting_error_upload
import me.matsumo.travelog.core.resource.map_setting_updating
import me.matsumo.travelog.core.resource.map_setting_uploading
import me.matsumo.travelog.feature.map.setting.MapSettingDialogState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MapSettingConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorState: MapSettingDialogState.Error? = null,
) {
    if (isError && errorState != null) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = {
                Text(text = stringResource(Res.string.common_error))
            },
            text = {
                Text(
                    text = when (errorState) {
                        MapSettingDialogState.Error.UpdateFailed -> stringResource(Res.string.map_setting_error_update)
                        MapSettingDialogState.Error.UploadFailed -> stringResource(Res.string.map_setting_error_upload)
                        MapSettingDialogState.Error.DeleteFailed -> stringResource(Res.string.map_setting_error_delete)
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(text = stringResource(Res.string.common_ok))
                }
            },
        )
    } else {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = {
                Text(text = stringResource(Res.string.map_setting_delete_title))
            },
            text = {
                Text(text = stringResource(Res.string.map_setting_delete_message))
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(text = stringResource(Res.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(Res.string.common_cancel))
                }
            },
        )
    }
}

@Composable
internal fun MapSettingLoadingDialog(
    loadingState: MapSettingDialogState.Loading,
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
                    text = when (loadingState) {
                        MapSettingDialogState.Loading.UploadingImage -> stringResource(Res.string.map_setting_uploading)
                        MapSettingDialogState.Loading.UpdatingMap -> stringResource(Res.string.map_setting_updating)
                        MapSettingDialogState.Loading.DeletingMap -> stringResource(Res.string.map_setting_deleting)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
