package me.matsumo.travelog.feature.map.photo.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.matsumo.travelog.core.model.db.ImageComment
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.common_unknown
import me.matsumo.travelog.core.resource.photo_detail_comments
import me.matsumo.travelog.core.resource.photo_detail_comments_empty
import me.matsumo.travelog.core.resource.photo_detail_comments_tap_to_add
import me.matsumo.travelog.core.ui.component.CommonSectionItem
import org.jetbrains.compose.resources.stringResource

@Suppress("DEPRECATION")
@Composable
internal fun PhotoDetailCommentSection(
    comments: ImmutableList<ImageComment>,
    onCommentClicked: (ImageComment?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 6.dp)
                .padding(horizontal = 16.dp),
            text = stringResource(Res.string.photo_detail_comments),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (comments.isEmpty()) {
                CommonSectionItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(Res.string.photo_detail_comments_empty),
                    description = stringResource(Res.string.photo_detail_comments_tap_to_add),
                    icon = Icons.Default.ChatBubble,
                    onClick = { onCommentClicked(null) }
                )
            } else {
                comments.forEach { comment ->
                    val timestamp = comment.updatedAt ?: comment.createdAt
                    val timestampText = timestamp?.let { instant ->
                        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                        "${localDateTime.year}/${localDateTime.monthNumber}/${localDateTime.dayOfMonth} ${localDateTime.hour}:${localDateTime.minute.toString().padStart(2, '0')}"
                    } ?: stringResource(Res.string.common_unknown)

                    CommonSectionItem(
                        modifier = Modifier.fillMaxWidth(),
                        title = comment.body,
                        description = timestampText,
                        icon = Icons.Default.ChatBubble,
                        actionIcon = Icons.Default.Edit,
                        onClick = { onCommentClicked(comment) },
                    )
                }
            }
        }
    }
}
