package me.matsumo.travelog.feature.home.create.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.home_map_select_country_hint
import me.matsumo.travelog.core.resource.unit_region
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
                    MapCreateRegionItem(
                        modifier = Modifier.fillMaxWidth(),
                        imageUrl = region.flagUrl,
                        title = stringResource(region.nameRes),
                        description = region.subRegionCount.toString() + stringResource(Res.string.unit_region),
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
