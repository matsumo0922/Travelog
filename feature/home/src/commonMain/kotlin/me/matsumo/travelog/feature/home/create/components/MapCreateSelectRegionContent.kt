package me.matsumo.travelog.feature.home.create.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.home_map_area
import me.matsumo.travelog.core.resource.home_map_region
import me.matsumo.travelog.core.resource.home_map_select_region_hint
import me.matsumo.travelog.core.resource.unit_region
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.screen.ScreenState
import me.matsumo.travelog.feature.home.create.MapCreateSelectRegionUiState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MapCreateSelectRegionContent(
    screenState: ScreenState<MapCreateSelectRegionUiState>,
    onRetryClicked: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
        retryAction = onRetryClicked,
    ) { uiState ->
        Column(modifier) {
            LazyVerticalGrid(
                modifier = Modifier.weight(1f),
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.home_map_region),
                    )
                }

                item {
                    MapCreateRegionItem(
                        modifier = Modifier.fillMaxWidth(),
                        imageUrl = uiState.region.flagUrl,
                        title = stringResource(uiState.region.nameRes),
                        description = uiState.region.subRegionCount.toString() + stringResource(Res.string.unit_region),
                        onClick = { },
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.home_map_area),
                    )
                }

                for (element in uiState.elements) {
                    item(key = element.id) {
                        MapCreateRegionItem(
                            modifier = Modifier.fillMaxWidth(),
                            imageUrl = "",
                            title = element.tags.name,
                            description = "admin level: ${element.tags.adminLevel}",
                            onClick = { },
                        )
                    }
                }
            }

            MapCreateHintItem(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                hint = stringResource(Res.string.home_map_select_region_hint),
            )
        }
    }
}