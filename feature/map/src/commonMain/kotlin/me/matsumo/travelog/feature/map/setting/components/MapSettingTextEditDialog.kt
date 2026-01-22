package me.matsumo.travelog.feature.map.setting.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import me.matsumo.travelog.core.resource.home_map_description_placeholder
import me.matsumo.travelog.core.resource.home_map_title_placeholder
import me.matsumo.travelog.core.resource.map_setting_edit_description
import me.matsumo.travelog.core.resource.map_setting_edit_description_description
import me.matsumo.travelog.core.resource.map_setting_edit_title
import me.matsumo.travelog.core.resource.map_setting_edit_title_description
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MapSettingTextEditDialog(
    titleType: MapSettingTextEditDialog.TitleType,
    currentValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(currentValue) }

    val (title, description, placeholder, singleLine) = when (titleType) {
        MapSettingTextEditDialog.TitleType.Title -> DialogConfig(
            title = Res.string.map_setting_edit_title,
            description = Res.string.map_setting_edit_title_description,
            placeholder = Res.string.home_map_title_placeholder,
            singleLine = true,
        )

        MapSettingTextEditDialog.TitleType.Description -> DialogConfig(
            title = Res.string.map_setting_edit_description,
            description = Res.string.map_setting_edit_description_description,
            placeholder = Res.string.home_map_description_placeholder,
            singleLine = false,
        )
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = stringResource(description))

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(stringResource(placeholder))
                    },
                    singleLine = singleLine,
                    minLines = if (singleLine) 1 else 3,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
            ) {
                Text(stringResource(Res.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        },
    )
}

object MapSettingTextEditDialog {
    enum class TitleType {
        Title,
        Description,
    }
}

private data class DialogConfig(
    val title: StringResource,
    val description: StringResource,
    val placeholder: StringResource,
    val singleLine: Boolean,
)
