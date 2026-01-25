package me.matsumo.travelog.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

private const val COLUMN_COUNT = 4

@Immutable
data class PlacedTileItem<T : TileGridItem>(
    val item: T,
    val column: Int,
    val row: Int,
    val spanWidth: Int,
    val spanHeight: Int,
)

@Composable
fun <T : TileGridItem> TileGrid(
    placedItems: ImmutableList<PlacedTileItem<T>>,
    rowCount: Int,
    modifier: Modifier = Modifier,
    cellSpacing: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    itemContent: @Composable (item: T) -> Unit,
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier.padding(contentPadding),
    ) {
        val density = LocalDensity.current
        val maxWidthPx = constraints.maxWidth
        val spacingPx = with(density) { cellSpacing.roundToPx() }
        val cellSizePx = (maxWidthPx - spacingPx * (COLUMN_COUNT - 1)) / COLUMN_COUNT
        val gridHeightPx = if (rowCount > 0) {
            cellSizePx * rowCount + spacingPx * (rowCount - 1)
        } else {
            0
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(scrollState),
        ) {
            Layout(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { gridHeightPx.toDp() }),
                content = {
                    placedItems.forEach { placed ->
                        key(placed.item.id) {
                            itemContent(placed.item)
                        }
                    }
                },
            ) { measurables, layoutConstraints ->
                val placeables = measurables.mapIndexed { index, measurable ->
                    val placed = placedItems[index]
                    val widthPx = cellSizePx * placed.spanWidth + spacingPx * (placed.spanWidth - 1)
                    val heightPx = cellSizePx * placed.spanHeight + spacingPx * (placed.spanHeight - 1)
                    measurable.measure(Constraints.fixed(widthPx, heightPx))
                }

                layout(layoutConstraints.maxWidth, gridHeightPx) {
                    placeables.forEachIndexed { index, placeable ->
                        val placed = placedItems[index]
                        val xPx = placed.column * (cellSizePx + spacingPx)
                        val yPx = placed.row * (cellSizePx + spacingPx)
                        placeable.place(xPx, yPx)
                    }
                }
            }
        }
    }
}
