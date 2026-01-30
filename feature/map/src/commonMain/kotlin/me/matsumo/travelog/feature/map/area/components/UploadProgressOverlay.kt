package me.matsumo.travelog.feature.map.area.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.map_upload_progress_multiple
import me.matsumo.travelog.core.resource.map_upload_progress_single
import me.matsumo.travelog.feature.map.area.UploadState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UploadProgressDialog(
    uploadState: UploadState,
    modifier: Modifier = Modifier,
) {
    val uploading = uploadState as? UploadState.Uploading ?: return

    AlertDialog(
        modifier = modifier,
        onDismissRequest = { },
        confirmButton = { },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (uploading.totalCount == 1) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(Res.string.map_upload_progress_single),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    val progress = if (uploading.totalCount > 0) {
                        uploading.completedCount.toFloat() / uploading.totalCount.toFloat()
                    } else {
                        0f
                    }

                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(48.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(
                            Res.string.map_upload_progress_multiple,
                            uploading.completedCount,
                            uploading.totalCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
    )
}
