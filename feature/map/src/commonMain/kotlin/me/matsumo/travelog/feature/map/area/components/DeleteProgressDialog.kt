package me.matsumo.travelog.feature.map.area.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.map_area_delete_progress
import me.matsumo.travelog.core.ui.theme.center
import me.matsumo.travelog.feature.map.area.DeleteState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeleteProgressDialog(
    deleteState: DeleteState,
) {
    val deleting = deleteState as? DeleteState.Deleting ?: return
    val progress = if (deleting.totalCount > 0) {
        deleting.completedCount.toFloat() / deleting.totalCount.toFloat()
    } else {
        0f
    }

    val progressAnimation by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "DeleteProgressDialog",
    )

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    space = 16.dp,
                    alignment = Alignment.CenterVertically,
                ),
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { progressAnimation },
                )

                Text(
                    text = stringResource(Res.string.map_area_delete_progress, deleting.completedCount, deleting.totalCount),
                    style = MaterialTheme.typography.bodyMedium.center(),
                )
            }
        },
    )
}
