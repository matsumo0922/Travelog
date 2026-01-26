package me.matsumo.travelog.feature.map.area.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.calf.permissions.ExperimentalPermissionsApi
import com.mohamedrejeb.calf.permissions.Permission
import com.mohamedrejeb.calf.permissions.isDenied
import com.mohamedrejeb.calf.permissions.isGranted
import com.mohamedrejeb.calf.permissions.isNotGranted
import com.mohamedrejeb.calf.permissions.rememberPermissionState
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.map_region_add_photo
import me.matsumo.travelog.core.ui.component.ClippedRegionImage
import me.matsumo.travelog.core.ui.component.GeoCanvasMap
import me.matsumo.travelog.core.ui.screen.Destination
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.core.ui.theme.semiBold
import me.matsumo.travelog.core.ui.utils.getLocalizedName
import me.matsumo.travelog.core.usecase.TempFileStorage
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun MapAreaDetailHeader(
    mapId: String,
    geoAreaId: String,
    geoArea: GeoArea,
    mapRegions: ImmutableList<MapRegion>,
    regionImageUrls: ImmutableMap<String, String>,
    existingRegionId: String?,
    tempFileStorage: TempFileStorage,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val navBackStack = LocalNavBackStack.current

    fun launchImagePicker() {
        scope.launch {
            runCatching {
                FileKit.openFilePicker(FileKitType.Image, FileKitMode.Single)?.also { file ->
                    val tempPath = tempFileStorage.saveToTemp(file)
                    navBackStack.add(
                        Destination.PhotoCropEditor(
                            mapId = mapId,
                            geoAreaId = geoAreaId,
                            localFilePath = tempPath,
                            existingRegionId = existingRegionId,
                        ),
                    )
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

    val regionMap = remember(mapRegions) {
        mapRegions.associateBy { it.geoAreaId }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = geoArea.getLocalizedName(),
                style = MaterialTheme.typography.headlineLarge.semiBold(),
            )

            Button(
                onClick = {
                    when {
                        galleryPermissionState.status.isGranted -> launchImagePicker()
                        galleryPermissionState.status.isNotGranted -> galleryPermissionState.launchPermissionRequest()
                        galleryPermissionState.status.isDenied -> galleryPermissionState.openAppSettings()
                    }
                },
            ) {
                Text(
                    text = stringResource(Res.string.map_region_add_photo),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            GeoCanvasMap(
                modifier = Modifier.fillMaxSize(),
                areas = geoArea.children.toImmutableList(),
                overlay = { mapState ->
                    geoArea.children.forEach { childArea ->
                        val childAreaId = childArea.id ?: return@forEach
                        val region = regionMap[childAreaId] ?: return@forEach

                        val croppedImageUrl = region.representativeCroppedImageId?.let { regionImageUrls[it] }
                        val originalImageUrl = region.representativeImageId?.let { regionImageUrls[it] }

                        val usePreCropped = croppedImageUrl != null
                        val imageUrl = croppedImageUrl ?: originalImageUrl ?: return@forEach

                        ClippedRegionImage(
                            modifier = Modifier.matchParentSize(),
                            imageUrl = imageUrl,
                            geoArea = childArea,
                            cropData = if (usePreCropped) null else region.cropData,
                            isPreCropped = usePreCropped,
                            parentBounds = mapState.bounds,
                            parentTransform = mapState.transform,
                        )
                    }
                },
            )
        }
    }
}
