package me.matsumo.travelog.core.ui.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

private const val COLUMN_COUNT = 4
private const val HEADER_INDEX = -1

@Immutable
data class PlacedTileItem<T : TileGridItem>(
    val item: T,
    val column: Int,
    val row: Int,
    val spanWidth: Int,
    val spanHeight: Int,
)

@Stable
class LazyTileGridState(
    initialScrollOffset: Float = 0f,
) : ScrollableState {

    private var _scrollOffset by mutableFloatStateOf(initialScrollOffset)
    val scrollOffset: Float get() = _scrollOffset

    private var _maxScrollOffset by mutableIntStateOf(0)
    val maxScrollOffset: Int get() = _maxScrollOffset

    private val scrollableState = ScrollableState { delta ->
        val newOffset = (_scrollOffset - delta).coerceIn(0f, _maxScrollOffset.toFloat())
        val consumed = _scrollOffset - newOffset
        _scrollOffset = newOffset
        consumed
    }

    internal fun updateMaxScrollOffset(value: Int) {
        _maxScrollOffset = value.coerceAtLeast(0)
        _scrollOffset = _scrollOffset.coerceIn(0f, _maxScrollOffset.toFloat())
    }

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    override fun dispatchRawDelta(delta: Float): Float =
        scrollableState.dispatchRawDelta(delta)

    override suspend fun scroll(
        scrollPriority: androidx.compose.foundation.MutatePriority,
        block: suspend androidx.compose.foundation.gestures.ScrollScope.() -> Unit,
    ) {
        scrollableState.scroll(scrollPriority, block)
    }

    companion object {
        val Saver: Saver<LazyTileGridState, Float> = Saver(
            save = { it.scrollOffset },
            restore = { LazyTileGridState(it) },
        )
    }
}

@Composable
fun rememberLazyTileGridState(): LazyTileGridState {
    return rememberSaveable(saver = LazyTileGridState.Saver) {
        LazyTileGridState()
    }
}

@Composable
fun <T : TileGridItem> TileGrid(
    placedItems: ImmutableList<PlacedTileItem<T>>,
    rowCount: Int,
    modifier: Modifier = Modifier,
    cellSpacing: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    state: LazyTileGridState = rememberLazyTileGridState(),
    header: (@Composable () -> Unit)? = null,
    itemContent: @Composable (item: T) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.padding(contentPadding),
    ) {
        val density = LocalDensity.current
        val maxWidthPx = constraints.maxWidth
        val viewportHeight = constraints.maxHeight
        val spacingPx = with(density) { cellSpacing.roundToPx() }
        val cellSizePx = (maxWidthPx - spacingPx * (COLUMN_COUNT - 1)) / COLUMN_COUNT
        val gridHeightPx = if (rowCount > 0) {
            cellSizePx * rowCount + spacingPx * (rowCount - 1)
        } else {
            0
        }

        var headerHeightPx by remember { mutableIntStateOf(0) }

        val itemProvider = remember(placedItems, header, itemContent) {
            TileGridItemProvider(
                placedItems = placedItems,
                hasHeader = header != null,
                itemContent = itemContent,
                headerContent = header,
            )
        }

        val visibleItemIndices by remember(placedItems, state, viewportHeight, cellSizePx, spacingPx, headerHeightPx) {
            derivedStateOf {
                calculateVisibleItemIndices(
                    placedItems = placedItems,
                    scrollOffset = state.scrollOffset.roundToInt(),
                    viewportHeight = viewportHeight,
                    cellSizePx = cellSizePx,
                    spacingPx = spacingPx,
                    headerHeightPx = headerHeightPx,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .scrollable(
                    state = state,
                    orientation = Orientation.Vertical,
                ),
        ) {
            LazyLayout(
                modifier = Modifier.fillMaxSize(),
                itemProvider = { itemProvider },
            ) { constraints ->
                val totalHeight = gridHeightPx + headerHeightPx
                state.updateMaxScrollOffset(totalHeight - viewportHeight)

                measureAndPlaceTileGrid(
                    constraints = constraints,
                    hasHeader = header != null,
                    placedItems = placedItems,
                    visibleItemIndices = visibleItemIndices,
                    scrollOffset = state.scrollOffset.roundToInt(),
                    cellSizePx = cellSizePx,
                    spacingPx = spacingPx,
                    viewportHeight = viewportHeight,
                    onHeaderMeasured = { headerHeightPx = it },
                )
            }
        }
    }
}

private class TileGridItemProvider<T : TileGridItem>(
    private val placedItems: ImmutableList<PlacedTileItem<T>>,
    private val hasHeader: Boolean,
    private val itemContent: @Composable (item: T) -> Unit,
    private val headerContent: (@Composable () -> Unit)?,
) : LazyLayoutItemProvider {

    override val itemCount: Int
        get() = placedItems.size + if (hasHeader) 1 else 0

    override fun getContentType(index: Int): Any {
        return if (hasHeader && index == 0) "header" else "item"
    }

    override fun getKey(index: Int): Any {
        return if (hasHeader && index == 0) {
            HEADER_INDEX
        } else {
            val itemIndex = if (hasHeader) index - 1 else index
            placedItems.getOrNull(itemIndex)?.item?.id ?: index
        }
    }

    @Composable
    override fun Item(index: Int, key: Any) {
        if (hasHeader && index == 0) {
            headerContent?.invoke()
        } else {
            val itemIndex = if (hasHeader) index - 1 else index
            placedItems.getOrNull(itemIndex)?.let { placed ->
                itemContent(placed.item)
            }
        }
    }
}

private fun <T : TileGridItem> calculateVisibleItemIndices(
    placedItems: ImmutableList<PlacedTileItem<T>>,
    scrollOffset: Int,
    viewportHeight: Int,
    cellSizePx: Int,
    spacingPx: Int,
    headerHeightPx: Int,
): List<Int> {
    if (placedItems.isEmpty() || cellSizePx <= 0) return emptyList()

    val gridScrollOffset = (scrollOffset - headerHeightPx).coerceAtLeast(0)
    val visibleTopPx = gridScrollOffset
    val visibleBottomPx = gridScrollOffset + viewportHeight

    val rowHeight = cellSizePx + spacingPx
    val firstVisibleRow = (visibleTopPx / rowHeight).coerceAtLeast(0)
    val lastVisibleRow = ((visibleBottomPx + rowHeight - 1) / rowHeight)

    return placedItems.indices.filter { index ->
        val placed = placedItems[index]
        val itemTopRow = placed.row
        val itemBottomRow = placed.row + placed.spanHeight - 1
        itemBottomRow >= firstVisibleRow && itemTopRow <= lastVisibleRow
    }
}

private fun <T : TileGridItem> LazyLayoutMeasureScope.measureAndPlaceTileGrid(
    constraints: Constraints,
    hasHeader: Boolean,
    placedItems: ImmutableList<PlacedTileItem<T>>,
    visibleItemIndices: List<Int>,
    scrollOffset: Int,
    cellSizePx: Int,
    spacingPx: Int,
    viewportHeight: Int,
    onHeaderMeasured: (Int) -> Unit,
): MeasureResult {
    var headerPlaceable: Placeable? = null
    var headerHeightPx = 0

    if (hasHeader) {
        val headerMeasurables = compose(0)
        if (headerMeasurables.isNotEmpty()) {
            headerPlaceable = headerMeasurables[0].measure(Constraints(maxWidth = constraints.maxWidth))
            headerHeightPx = headerPlaceable.height
            onHeaderMeasured(headerHeightPx)
        }
    }

    val itemPlaceables = mutableListOf<Pair<Int, Placeable>>()

    for (itemIndex in visibleItemIndices) {
        val placed = placedItems[itemIndex]
        val providerIndex = if (hasHeader) itemIndex + 1 else itemIndex

        val widthPx = cellSizePx * placed.spanWidth + spacingPx * (placed.spanWidth - 1)
        val heightPx = cellSizePx * placed.spanHeight + spacingPx * (placed.spanHeight - 1)
        val measurables = compose(providerIndex)

        if (measurables.isNotEmpty()) {
            val placeable = measurables[0].measure(Constraints.fixed(widthPx, heightPx))
            itemPlaceables.add(itemIndex to placeable)
        }
    }

    return layout(constraints.maxWidth, viewportHeight) {
        headerPlaceable?.let { header ->
            val headerY = -scrollOffset
            if (headerY + headerHeightPx > 0) {
                header.place(0, headerY)
            }
        }

        for ((itemIndex, placeable) in itemPlaceables) {
            val placed = placedItems[itemIndex]
            val xPx = placed.column * (cellSizePx + spacingPx)
            val yPx = placed.row * (cellSizePx + spacingPx) + headerHeightPx - scrollOffset
            placeable.place(xPx, yPx)
        }
    }
}
