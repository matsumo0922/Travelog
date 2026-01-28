package me.matsumo.travelog.core.ui.component

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

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

    // 普通の var に変更（Compose State ではない）- measure 中の更新で recomposition を trigger しない
    private var _maxScrollOffset: Int = 0
    val maxScrollOffset: Int get() = _maxScrollOffset

    private val scrollableState = ScrollableState { delta ->
        val newOffset = (_scrollOffset - delta).coerceIn(0f, _maxScrollOffset.toFloat())
        val consumed = _scrollOffset - newOffset
        _scrollOffset = newOffset
        consumed
    }

    // measure 中から呼ばれても recomposition を trigger しない
    internal fun updateMaxScrollOffset(value: Int) {
        _maxScrollOffset = value.coerceAtLeast(0)
        // maxScrollOffset が縮小した場合に scrollOffset が範囲外にならないようクランプ
        if (_scrollOffset > _maxScrollOffset) {
            _scrollOffset = _maxScrollOffset.toFloat()
        }
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
    columnCount: Int = 4,
    cornerRadius: Dp = 0.dp,
    cellSpacing: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    state: LazyTileGridState = rememberLazyTileGridState(),
    header: (@Composable () -> Unit)? = null,
    onItemClick: ((item: T) -> Unit)? = null,
    itemContent: @Composable (item: T) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.padding(contentPadding),
    ) {
        val density = LocalDensity.current
        val maxWidthPx = constraints.maxWidth
        val viewportHeight = constraints.maxHeight
        val spacingPx = with(density) { cellSpacing.roundToPx() }
        val cellSizePx = (maxWidthPx - spacingPx * (columnCount - 1)) / columnCount
        val gridHeightPx = if (rowCount > 0) {
            cellSizePx * rowCount + spacingPx * (rowCount - 1)
        } else {
            0
        }

        var headerHeightPx by remember { mutableIntStateOf(0) }

        // rememberUpdatedState で最新の itemContent/header/onItemClick を保持（ラムダ参照が変わっても追従）
        val currentItemContent by rememberUpdatedState(itemContent)
        val currentHeader by rememberUpdatedState(header)
        val currentOnItemClick by rememberUpdatedState(onItemClick)

        // placedItems と hasHeader と cornerRadius だけをキーにし、コンテンツは rememberUpdatedState 経由で実行時に最新を参照
        val itemProvider = remember(placedItems, header != null, cornerRadius) {
            TileGridItemProvider(
                placedItems = placedItems,
                hasHeader = header != null,
                cornerRadius = cornerRadius,
                itemContent = { currentItemContent(it) },
                headerContent = { currentHeader?.invoke() },
                onItemClick = { currentOnItemClick?.invoke(it) },
            )
        }

        // placedItems が変わったときに一度だけ構築する行→アイテムインデックスのマップ
        val rowToItemIndices = remember(placedItems) {
            buildRowToItemIndicesMap(placedItems)
        }

        // 可視行のみを走査 O(visibleRows + visibleItems) に最適化
        val visibleItemIndices by remember(rowToItemIndices, rowCount, state, viewportHeight, cellSizePx, spacingPx, headerHeightPx) {
            derivedStateOf {
                val (firstVisibleRow, lastVisibleRow) = calculateVisibleRows(
                    scrollOffset = state.scrollOffset.roundToInt(),
                    viewportHeight = viewportHeight,
                    cellSizePx = cellSizePx,
                    spacingPx = spacingPx,
                    headerHeightPx = headerHeightPx,
                    rowCount = rowCount,
                )
                calculateVisibleItemIndices(
                    rowToItemIndices = rowToItemIndices,
                    firstVisibleRow = firstVisibleRow,
                    lastVisibleRow = lastVisibleRow,
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
                    // 値が変わった時だけ更新（毎回 state 更新を避ける）
                    onHeaderMeasured = { newHeight ->
                        if (headerHeightPx != newHeight) {
                            headerHeightPx = newHeight
                        }
                    },
                )
            }
        }
    }
}

private class TileGridItemProvider<T : TileGridItem>(
    private val placedItems: ImmutableList<PlacedTileItem<T>>,
    private val hasHeader: Boolean,
    private val cornerRadius: Dp,
    private val itemContent: @Composable (item: T) -> Unit,
    private val headerContent: (@Composable () -> Unit)?,
    private val onItemClick: ((item: T) -> Unit)?,
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (cornerRadius > 0.dp) {
                                Modifier.clip(RoundedCornerShape(cornerRadius))
                            } else {
                                Modifier
                            }
                        )
                        .clickable(enabled = onItemClick != null) { onItemClick?.invoke(placed.item) },
                ) {
                    itemContent(placed.item)
                }
            }
        }
    }
}

/**
 * placedItems から行→アイテムインデックスのマップを構築する。
 * O(n) だが、placedItems が変わったときに一度だけ実行される。
 */
private fun <T : TileGridItem> buildRowToItemIndicesMap(
    placedItems: ImmutableList<PlacedTileItem<T>>,
): Map<Int, List<Int>> {
    val map = mutableMapOf<Int, MutableList<Int>>()
    placedItems.forEachIndexed { index, placed ->
        // アイテムが占有する全ての行に登録
        for (row in placed.row until placed.row + placed.spanHeight) {
            map.getOrPut(row) { mutableListOf() }.add(index)
        }
    }
    return map
}

/**
 * 可視行の範囲を計算する。
 * 早期リターンで無効なパラメータを処理。
 */
private fun calculateVisibleRows(
    scrollOffset: Int,
    viewportHeight: Int,
    cellSizePx: Int,
    spacingPx: Int,
    headerHeightPx: Int,
    rowCount: Int,
): Pair<Int, Int> {
    // 早期 return
    if (viewportHeight <= 0 || cellSizePx <= 0 || rowCount <= 0) {
        return 0 to -1 // 空の範囲
    }

    val gridScrollOffset = (scrollOffset - headerHeightPx).coerceAtLeast(0)
    val rowHeight = cellSizePx + spacingPx

    val firstVisibleRow = (gridScrollOffset / rowHeight).coerceAtLeast(0)
    val lastVisibleRow = ((gridScrollOffset + viewportHeight + rowHeight - 1) / rowHeight)
        .coerceAtMost(rowCount - 1) // rowCount で clamp

    return firstVisibleRow to lastVisibleRow
}

/**
 * 可視行のみを走査して可視アイテムのインデックスを返す。
 * O(visibleRows + visibleItems) で完了。
 */
private fun calculateVisibleItemIndices(
    rowToItemIndices: Map<Int, List<Int>>,
    firstVisibleRow: Int,
    lastVisibleRow: Int,
): Set<Int> {
    if (firstVisibleRow > lastVisibleRow) return emptySet()

    val result = mutableSetOf<Int>()
    for (row in firstVisibleRow..lastVisibleRow) {
        rowToItemIndices[row]?.let { result.addAll(it) }
    }
    return result
}

private fun <T : TileGridItem> LazyLayoutMeasureScope.measureAndPlaceTileGrid(
    constraints: Constraints,
    hasHeader: Boolean,
    placedItems: ImmutableList<PlacedTileItem<T>>,
    visibleItemIndices: Set<Int>,
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
