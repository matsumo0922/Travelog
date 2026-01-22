package me.matsumo.travelog.feature.map.setting.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.matsumo.travelog.core.model.db.Map
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.map_setting_details
import me.matsumo.travelog.core.resource.map_setting_details_created_at
import me.matsumo.travelog.core.resource.map_setting_details_delete
import me.matsumo.travelog.core.resource.map_setting_details_delete_hint
import me.matsumo.travelog.core.resource.map_setting_details_geo_area_id
import me.matsumo.travelog.core.resource.map_setting_details_map_id
import me.matsumo.travelog.core.ui.component.CommonSectionItem
import org.jetbrains.compose.resources.stringResource

@Suppress("DEPRECATION")
@Composable
internal fun MapSettingDetailsSection(
    map: Map,
    geoAreaId: String,
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val createdAtText = map.createdAt?.let { instant ->
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.year}/${localDateTime.monthNumber}/${localDateTime.dayOfMonth} ${localDateTime.hour}:${
            localDateTime.minute.toString().padStart(2, '0')
        }"
    }.orEmpty()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 6.dp)
                .padding(horizontal = 16.dp),
            text = stringResource(Res.string.map_setting_details),
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
                title = stringResource(Res.string.map_setting_details_map_id),
                description = map.id.orEmpty(),
                icon = Icons.Default.Key,
            )

            CommonSectionItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.map_setting_details_geo_area_id),
                description = geoAreaId,
                icon = Icons.Default.Map,
            )

            CommonSectionItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.map_setting_details_created_at),
                description = createdAtText,
                icon = Icons.Default.CalendarToday,
            )

            CommonSectionItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.map_setting_details_delete),
                description = stringResource(Res.string.map_setting_details_delete_hint),
                icon = Icons.Default.Delete,
                onClick = onDeleteClicked,
            )
        }
    }
}
