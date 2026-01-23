package me.matsumo.travelog.feature.map.photo.components

import me.matsumo.travelog.feature.map.photo.components.model.GridPhotoItem
import me.matsumo.travelog.feature.map.photo.components.model.GridSpanConfig
import me.matsumo.travelog.feature.map.photo.components.model.PlacedGridItem

data class GridPlacementResult(
    val placedItems: List<PlacedGridItem>,
    val rowCount: Int,
)

class GridPlacer(private val config: GridSpanConfig) {

    private val columnCount = config.columnCount
    private val occupiedCells = mutableListOf<BooleanArray>()

    fun placeItems(items: List<GridPhotoItem>): GridPlacementResult {
        occupiedCells.clear()
        val placedItems = mutableListOf<PlacedGridItem>()

        for (item in items) {
            val placed = placeItem(item)
            placedItems.add(placed)
        }

        val rowCount = occupiedCells.size
        return GridPlacementResult(placedItems, rowCount)
    }

    private fun placeItem(item: GridPhotoItem): PlacedGridItem {
        var spanW = item.spanWidth.coerceIn(1, columnCount)
        var spanH = item.spanHeight.coerceAtLeast(1)

        var position = findPosition(spanW, spanH)

        if (position == null) {
            spanW = 1
            spanH = 1
            position = findPosition(1, 1)!!
        }

        markOccupied(position.first, position.second, spanW, spanH)

        return PlacedGridItem(
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
