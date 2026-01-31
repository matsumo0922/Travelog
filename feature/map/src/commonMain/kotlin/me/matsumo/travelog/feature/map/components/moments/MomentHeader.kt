package me.matsumo.travelog.feature.map.components.moments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.matsumo.travelog.core.model.DateRange
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.moment_photo_count
import me.matsumo.travelog.core.ui.theme.semiBold
import me.matsumo.travelog.core.ui.utils.getLocalizedName
import org.jetbrains.compose.resources.stringResource

/**
 * Header component for a moment card.
 * Displays metadata (date range, photo count) and the region name.
 */
@Composable
internal fun MomentHeader(
    geoArea: GeoArea,
    dateRange: DateRange?,
    imageCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Metadata row - Label Medium
        MetadataRow(
            dateRange = dateRange,
            imageCount = imageCount,
        )

        // Title - Headline Medium, SemiBold
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = geoArea.getLocalizedName(),
            style = MaterialTheme.typography.headlineMedium.semiBold(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetadataRow(
    dateRange: DateRange?,
    imageCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Date range
        if (dateRange != null) {
            Text(
                text = formatDateRange(dateRange),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "•",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Image count
        Text(
            text = stringResource(Res.string.moment_photo_count, imageCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Suppress("DEPRECATION")
private fun formatDateRange(dateRange: DateRange): String {
    val timeZone = TimeZone.currentSystemDefault()
    val earliest = dateRange.earliest.toLocalDateTime(timeZone)
    val latest = dateRange.latest.toLocalDateTime(timeZone)

    val earliestStr = "${earliest.year}/${earliest.monthNumber}/${earliest.dayOfMonth}"

    return if (earliest.date == latest.date) {
        earliestStr
    } else {
        val latestStr = if (earliest.year == latest.year) {
            "${latest.monthNumber}/${latest.dayOfMonth}"
        } else {
            "${latest.year}/${latest.monthNumber}/${latest.dayOfMonth}"
        }
        "$earliestStr〜$latestStr"
    }
}
