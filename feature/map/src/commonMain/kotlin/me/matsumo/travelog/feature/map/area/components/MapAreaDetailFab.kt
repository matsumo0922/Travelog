package me.matsumo.travelog.feature.map.area.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.map_photo_add
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun MapAreaDetailFab(
    onImagePicked: (PlatformFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    fun launchImagePicker() {
        scope.launch {
            runCatching {
                FileKit.openFilePicker(FileKitType.Image, FileKitMode.Single)?.also { file ->
                    onImagePicked(file)
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

    ExtendedFloatingActionButton(
        modifier = modifier,
        onClick = {
            when {
                galleryPermissionState.status.isGranted -> launchImagePicker()
                galleryPermissionState.status.isNotGranted -> galleryPermissionState.launchPermissionRequest()
                galleryPermissionState.status.isDenied -> galleryPermissionState.openAppSettings()
            }
        },
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = "Reviews",
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(Res.string.map_photo_add),
        )
    }
}
