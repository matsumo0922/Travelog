package me.matsumo.travelog.feature.home.create.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.home_map_select_country_hint
import me.matsumo.travelog.core.resource.unit_region
import me.matsumo.travelog.core.ui.theme.semiBold
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MapCreateSelectCountryContent(
    onCountrySelected: (SupportedRegion) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        LazyVerticalGrid(
            modifier = Modifier.weight(1f),
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            for (region in SupportedRegion.all) {
                item(key = region.code) {
                    RegionItem(
                        modifier = Modifier.fillMaxWidth(),
                        region = region,
                        onClick = { onCountrySelected(region) },
                    )
                }
            }
        }

        MapCreateHintItem(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            hint = stringResource(Res.string.home_map_select_country_hint),
        )
    }
}

@Composable
private fun RegionItem(
    region: SupportedRegion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
        onClick = onClick,
    ) {
        Box {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3 / 2f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    model = region.flagUrl,
                    contentScale = ContentScale.Fit,
                    contentDescription = null,
                )

                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(region.nameRes),
                        style = MaterialTheme.typography.titleMedium.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = region.subRegionCount.toString() + stringResource(Res.string.unit_region),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}