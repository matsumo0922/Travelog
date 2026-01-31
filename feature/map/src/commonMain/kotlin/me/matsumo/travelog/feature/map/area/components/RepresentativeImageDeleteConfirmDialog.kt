package me.matsumo.travelog.feature.map.area.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.common_cancel
import me.matsumo.travelog.core.resource.common_delete
import me.matsumo.travelog.core.resource.map_region_delete_photo_message
import me.matsumo.travelog.core.resource.map_region_delete_photo_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RepresentativeImageDeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.map_region_delete_photo_title))
        },
        text = {
            Text(text = stringResource(Res.string.map_region_delete_photo_message))
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
