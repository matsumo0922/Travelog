package me.matsumo.travelog.feature.home.create.metadata.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mohamedrejeb.calf.permissions.ExperimentalPermissionsApi
import com.mohamedrejeb.calf.permissions.Permission
import com.mohamedrejeb.calf.permissions.isDenied
import com.mohamedrejeb.calf.permissions.isGranted
import com.mohamedrejeb.calf.permissions.isNotGranted
import com.mohamedrejeb.calf.permissions.rememberPermissionState
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.home_map_description_description
import me.matsumo.travelog.core.resource.home_map_description_placeholder
import me.matsumo.travelog.core.resource.home_map_description_title
import me.matsumo.travelog.core.resource.home_map_icon_description
import me.matsumo.travelog.core.resource.home_map_icon_tap
import me.matsumo.travelog.core.resource.home_map_icon_title
import me.matsumo.travelog.core.resource.home_map_metadata
import me.matsumo.travelog.core.resource.home_map_title_description
import me.matsumo.travelog.core.resource.home_map_title_placeholder
import me.matsumo.travelog.core.resource.home_map_title_title
import me.matsumo.travelog.core.ui.component.CommonSectionItem
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MapCreateMetadataSection(
    title: String,
    description: String,
    iconFile: PlatformFile?,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onIconFileChange: (PlatformFile?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleState = remember { TextFieldState(title) }
    val descriptionState = remember { TextFieldState(description) }

    LaunchedEffect(titleState) {
        snapshotFlow { titleState.text.toString() }
            .collectLatest { onTitleChange(it) }
    }

    LaunchedEffect(descriptionState) {
        snapshotFlow { descriptionState.text.toString() }
            .collectLatest { onDescriptionChange(it) }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 6.dp)
                .padding(horizontal = 16.dp),
            text = stringResource(Res.string.home_map_metadata),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            CommonSectionItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.home_map_title_title),
                description = stringResource(Res.string.home_map_title_description),
                icon = Icons.Default.Edit,
                extra = {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        state = titleState,
                        lineLimits = TextFieldLineLimits.SingleLine,
                        placeholder = {
                            Text(stringResource(Res.string.home_map_title_placeholder))
                        }
                    )
                }
            )

            CommonSectionItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.home_map_description_title),
                description = stringResource(Res.string.home_map_description_description),
                icon = Icons.Default.Description,
                extra = {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        state = descriptionState,
                        lineLimits = TextFieldLineLimits.MultiLine(),
                        placeholder = {
                            Text(stringResource(Res.string.home_map_description_placeholder))
                        }
                    )
                }
            )

            CommonSectionItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.home_map_icon_title),
                description = stringResource(Res.string.home_map_icon_description),
                icon = Icons.Default.Image,
                extra = {
                    IconSelectItem(
                        modifier = Modifier
                            .aspectRatio(3 / 2f)
                            .fillMaxWidth(),
                        selectedFile = iconFile,
                        onFileSelected = onIconFileChange,
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun IconSelectItem(
    selectedFile: PlatformFile?,
    onFileSelected: (PlatformFile?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    fun launchImagePicker() {
        scope.launch {
            runCatching {
                FileKit.openFilePicker(FileKitType.Image, FileKitMode.Single)?.also { file ->
                    onFileSelected(file)
                }
            }.onFailure {
                Napier.e(it) { "Failed to pick image" }
            }
        }
    }

    val galleryPermissionState = rememberPermissionState(
        permission = Permission.Gallery,
        onPermissionResult = { isGrant ->
            if (isGrant) launchImagePicker()
        },
    )

    Column(
        modifier = modifier
            .clip(OutlinedTextFieldDefaults.shape)
            .border(
                width = if (selectedFile == null) 1.dp else 0.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = OutlinedTextFieldDefaults.shape,
            )
            .clickable {
                when {
                    galleryPermissionState.status.isGranted -> launchImagePicker()
                    galleryPermissionState.status.isNotGranted -> galleryPermissionState.launchPermissionRequest()
                    galleryPermissionState.status.isDenied -> galleryPermissionState.openAppSettings()
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        if (selectedFile == null) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Default.AddPhotoAlternate,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = null,
            )

            Text(
                text = stringResource(Res.string.home_map_icon_tap),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = selectedFile,
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
        }
    }
}
