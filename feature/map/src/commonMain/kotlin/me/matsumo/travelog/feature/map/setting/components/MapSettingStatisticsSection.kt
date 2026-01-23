package me.matsumo.travelog.feature.map.setting.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.map_setting_statistics
import me.matsumo.travelog.core.resource.map_setting_statistics_fill_percentage
import me.matsumo.travelog.core.resource.map_setting_statistics_photo_count
import me.matsumo.travelog.core.resource.map_setting_statistics_region_name
import me.matsumo.travelog.core.ui.component.CommonSectionItem
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MapSettingStatisticsSection(
    geoArea: GeoArea,
    regionCount: Int,
    totalChildCount: Int,
    photoCount: Int,
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
            text = stringResource(Res.string.map_setting_statistics),
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
                title = stringResource(Res.string.map_setting_statistics_region_name),
                description = geoArea.getLocalizedName(),
                icon = Icons.Default.Map,
            )

            CommonSectionItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.map_setting_statistics_fill_percentage),
                description = "$regionCount / $totalChildCount",
                icon = Icons.Default.Place,
            )

            CommonSectionItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.map_setting_statistics_photo_count),
                description = "$photoCount",
                icon = Icons.Default.Photo,
            )
        }
    }
}
