package me.matsumo.travelog.feature.map.photo.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.map_region_add_photo
import me.matsumo.travelog.core.ui.component.GeoCanvasMap
import me.matsumo.travelog.core.ui.theme.semiBold
import me.matsumo.travelog.core.ui.utils.getLocalizedName
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MapAddPhotoHeader(
    geoArea: GeoArea,
    modifier: Modifier = Modifier,
) {
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

            Button({}) {
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
            )
        }
    }
}