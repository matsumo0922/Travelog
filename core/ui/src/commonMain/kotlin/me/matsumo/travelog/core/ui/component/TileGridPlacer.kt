package me.matsumo.travelog.core.ui.component

data class TileGridPlacementResult<T : TileGridItem>(
    val placedItems: List<PlacedTileItem<T>>,
    val rowCount: Int,
)

class TileGridPlacer(private val config: TileGridConfig) {

    private val columnCount = config.columnCount
    private val occupiedCells = mutableListOf<BooleanArray>()

    fun <T : TileGridItem> placeItems(items: List<T>): TileGridPlacementResult<T> {
        occupiedCells.clear()
        val placedItems = mutableListOf<PlacedTileItem<T>>()

        for (item in items) {
            val placed = placeItem(item)
            placedItems.add(placed)
        }

        val rowCount = occupiedCells.size
        return TileGridPlacementResult(placedItems, rowCount)
    }

    private fun <T : TileGridItem> placeItem(item: T): PlacedTileItem<T> {
        var spanW = item.spanWidth.coerceIn(1, columnCount)
        var spanH = item.spanHeight.coerceAtLeast(1)

        var position = findPosition(spanW, spanH)

        if (position == null) {
            spanW = 1
            spanH = 1
            position = findPosition(1, 1)!!
        }

        markOccupied(position.first, position.second, spanW, spanH)

        return PlacedTileItem(
            item = item,
            column = position.first,
            row = position.second,
            spanWidth = spanW,
            spanHeight = spanH,
        )
    }

    private fun findPosition(spanWidth: Int, spanHeight: Int): Pair<Int, Int>? {
        ensureRows(occupiedCells.size + spanHeight)

        for (row in 0 until occupiedCells.size) {
            for (col in 0..columnCount - spanWidth) {
                if (canPlace(col, row, spanWidth, spanHeight)) {
                    return col to row
                }
            }
        }
        return null
    }

    private fun canPlace(col: Int, row: Int, spanWidth: Int, spanHeight: Int): Boolean {
        ensureRows(row + spanHeight)

        for (r in row until row + spanHeight) {
            for (c in col until col + spanWidth) {
                if (c >= columnCount || occupiedCells[r][c]) {
                    return false
                }
            }
        }
        return true
    }

    private fun markOccupied(col: Int, row: Int, spanWidth: Int, spanHeight: Int) {
        ensureRows(row + spanHeight)

        for (r in row until row + spanHeight) {
            for (c in col until col + spanWidth) {
                occupiedCells[r][c] = true
            }
        }
    }

    private fun ensureRows(minRows: Int) {
        while (occupiedCells.size < minRows) {
            occupiedCells.add(BooleanArray(columnCount) { false })
        }
    }
}
