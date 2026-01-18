package me.matsumo.travelog.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.collections.immutable.ImmutableList
import me.matsumo.travelog.core.model.geo.GeoRegion
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * A Composable that displays enriched region polygons data with zoom and pan capabilities
 *
 * @param regions The region polygons to display
 * @param modifier Modifier for the canvas
 * @param strokeColor Color for polygons borders
 * @param fillColor Color for polygons fill
 * @param strokeWidth Width of polygons borders
 */
@Composable
fun GeoCanvasMap(
    regions: ImmutableList<GeoRegion>,
    modifier: Modifier = Modifier,
    strokeColor: Color = Color.Black,
    fillColor: Color = Color.Gray.copy(alpha = 0.3f),
    strokeWidth: Float = 0.1f,
) {
    val zoomState = rememberZoomState()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Calculate bounding box from region polygons
    val bounds = remember(regions) {
        GeoJsonRenderer.calculateBounds(regions)
    }

    // Calculate viewport transform to maintain aspect ratio
    val viewportTransform = remember(bounds, canvasSize) {
        if (bounds == null || canvasSize.width == 0 || canvasSize.height == 0) {
            null
        } else {
            GeoJsonRenderer.calculateViewportTransform(
                bounds = bounds,
                canvasWidth = canvasSize.width.toFloat(),
                canvasHeight = canvasSize.height.toFloat(),
            )
        }
    }

    // Pre-compute paths for better performance
    val paths by remember(regions, bounds, viewportTransform) {
        derivedStateOf {
            if (bounds == null || viewportTransform == null) {
                return@derivedStateOf emptyList()
            }
            GeoJsonRenderer.createPaths(
                regions = regions,
                bounds = bounds,
                transform = viewportTransform,
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .zoomable(zoomState),
    ) {
        drawGeoJson(
            paths = paths,
            strokeColor = strokeColor,
            fillColor = fillColor,
            strokeWidth = strokeWidth,
        )
    }
}

/**
 * Draw region paths on the canvas
 */
private fun DrawScope.drawGeoJson(
    paths: List<androidx.compose.ui.graphics.Path>,
    strokeColor: Color,
    fillColor: Color,
    strokeWidth: Float,
) {
    paths.forEach { path ->
        // Draw filled polygons
        drawPath(
            path = path,
            color = fillColor,
        )

        // Draw border
        drawPath(
            path = path,
            color = strokeColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
