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
import me.matsumo.travelog.core.model.geo.GeoArea
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * A Composable that displays enriched region polygons data with zoom and pan capabilities
 *
 * @param areas The area polygons to display
 * @param modifier Modifier for the canvas
 * @param strokeColor Color for polygons borders
 * @param fillColor Color for polygons fill
 * @param strokeWidth Width of polygons borders
 */
@Composable
fun GeoCanvasMap(
    areas: ImmutableList<GeoArea>,
    modifier: Modifier = Modifier,
    strokeColor: Color = Color(0xFF33691E),
    fillColor: Color = Color(0xFF9CCC65).copy(alpha = 0.6f),
    strokeWidth: Float = 0.5f,
    enableZoom: Boolean = true,
) {
    val zoomState = rememberZoomState()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Calculate bounding box from area polygons
    val bounds = remember(areas) {
        GeoJsonRenderer.calculateBounds(areas)
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
    val paths by remember(areas, bounds, viewportTransform) {
        derivedStateOf {
            if (bounds == null || viewportTransform == null) {
                return@derivedStateOf emptyList()
            }
            GeoJsonRenderer.createPaths(
                areas = areas,
                bounds = bounds,
                transform = viewportTransform,
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .then(if (enableZoom) Modifier.zoomable(zoomState) else Modifier),
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
