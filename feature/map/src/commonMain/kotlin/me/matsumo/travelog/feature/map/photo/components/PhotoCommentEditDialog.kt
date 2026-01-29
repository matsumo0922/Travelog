package me.matsumo.travelog.feature.map.photo.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.common_cancel
import me.matsumo.travelog.core.resource.common_save
import me.matsumo.travelog.core.resource.photo_detail_comment_edit_placeholder
import me.matsumo.travelog.core.resource.photo_detail_comment_edit_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PhotoCommentEditDialog(
    currentValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(currentValue) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.photo_detail_comment_edit_title))
        },
        text = {
            OutlinedTextField(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(stringResource(Res.string.photo_detail_comment_edit_placeholder))
                },
                minLines = 3,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
            ) {
                Text(stringResource(Res.string.common_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(Res.string.common_cancel))
            }
        },
    )
}
