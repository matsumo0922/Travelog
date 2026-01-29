package me.matsumo.travelog.feature.map.photo.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.matsumo.travelog.core.common.formatCacheSize
import me.matsumo.travelog.core.model.db.Image
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.common_unknown
import me.matsumo.travelog.core.resource.photo_detail_metadata
import me.matsumo.travelog.core.resource.photo_detail_metadata_exif
import me.matsumo.travelog.core.resource.photo_detail_metadata_saved_at
import me.matsumo.travelog.core.resource.photo_detail_metadata_size
import me.matsumo.travelog.core.resource.photo_detail_metadata_storage
import me.matsumo.travelog.core.ui.component.CommonSectionItem
import org.jetbrains.compose.resources.stringResource

@Suppress("DEPRECATION")
@Composable
internal fun PhotoDetailMetadataSection(
    image: Image?,
    modifier: Modifier = Modifier,
) {
    val sizeText = if (image?.width != null && image.height != null) {
        "${image.width} x ${image.height} px"
    } else {
        stringResource(Res.string.common_unknown)
    }

    val storageText = if (image?.fileSize != null) {
        formatCacheSize(image.fileSize)
    } else {
        stringResource(Res.string.common_unknown)
    }

    val savedAtText = image?.createdAt?.let { instant ->
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.year}/${localDateTime.monthNumber}/${localDateTime.dayOfMonth} ${localDateTime.hour}:${localDateTime.minute.toString().padStart(2, '0')}"
    } ?: stringResource(Res.string.common_unknown)

    val exifText = image?.exif?.toString() ?: stringResource(Res.string.common_unknown)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 6.dp)
                .padding(horizontal = 16.dp),
            text = stringResource(Res.string.photo_detail_metadata),
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
            CommonSectionItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.photo_detail_metadata_size),
                description = sizeText,
                icon = Icons.Default.AspectRatio,
            )

            CommonSectionItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.photo_detail_metadata_storage),
                description = storageText,
                icon = Icons.Default.Storage,
            )

            CommonSectionItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.photo_detail_metadata_saved_at),
                description = savedAtText,
                icon = Icons.Default.CalendarToday,
            )

            CommonSectionItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.photo_detail_metadata_exif),
                description = null,
                icon = Icons.Default.Description,
                extra = {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = exifText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}
