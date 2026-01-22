package me.matsumo.travelog.feature.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import me.matsumo.travelog.core.model.db.Map
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.map_photo_list
import me.matsumo.travelog.core.ui.theme.semiBold
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MapDetailTopSection(
    map: Map,
    area: GeoArea,
    regions: ImmutableList<MapRegion>,
    onPhotosClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "${area.getLocalizedName(Locale.current.language == "ja")}・${regions.size}/${area.childCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = map.title,
                style = MaterialTheme.typography.headlineLarge.semiBold(),
            )

            Button(onPhotosClicked) {
                Text(
                    text = stringResource(Res.string.map_photo_list),
                )
            }
        }
    }
}
