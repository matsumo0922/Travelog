package me.matsumo.travelog.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
 * GeoCanvasMap の overlay 内で利用可能な状態
 */
@Immutable
data class GeoCanvasMapState(
    val bounds: GeoJsonRenderer.Bounds,
    val transform: GeoJsonRenderer.ViewportTransform,
)

/**
 * A Composable that displays enriched region polygons data with zoom and pan capabilities
 *
 * @param areas The area polygons to display
 * @param modifier Modifier for the canvas
 * @param strokeColor Color for polygons borders
 * @param fillColor Color for polygons fill
 * @param strokeWidth Width of polygons borders
 * @param enableZoom Whether to enable zoom and pan
 * @param externalBounds External bounds to use (for coordinate system unification)
 * @param externalTransform External transform to use (for coordinate system unification)
 * @param overlay Composable slot for overlaying content (e.g., images) that will zoom/pan with the map
 */
@Composable
fun GeoCanvasMap(
    areas: ImmutableList<GeoArea>,
    modifier: Modifier = Modifier,
    strokeColor: Color = Color(0xFF33691E),
    fillColor: Color = Color(0xFF9CCC65).copy(alpha = 0.6f),
    strokeWidth: Float = 0.5f,
    enableZoom: Boolean = true,
    externalBounds: GeoJsonRenderer.Bounds? = null,
    externalTransform: GeoJsonRenderer.ViewportTransform? = null,
    overlay: @Composable BoxScope.(GeoCanvasMapState) -> Unit = {},
) {
    val zoomState = rememberZoomState()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Use external bounds if provided, otherwise calculate from areas
    val bounds = remember(areas, externalBounds) {
        externalBounds ?: GeoJsonRenderer.calculateBounds(areas)
    }

    // Use external transform if provided, otherwise calculate from bounds
    val viewportTransform = remember(bounds, canvasSize, externalTransform) {
        externalTransform ?: if (bounds == null || canvasSize.width == 0 || canvasSize.height == 0) {
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

    // Box でラップして zoomable を適用
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .then(if (enableZoom) Modifier.zoomable(zoomState) else Modifier),
    ) {
        // 1. Fill を最下層に描画（地域の塗りつぶし）
        Canvas(modifier = Modifier.matchParentSize()) {
            drawGeoJsonFill(
                paths = paths,
                fillColor = fillColor,
            )
        }

        // 2. Overlay を中間層に描画（画像など）
        if (bounds != null && viewportTransform != null) {
            val mapState = remember(bounds, viewportTransform) {
                GeoCanvasMapState(bounds, viewportTransform)
            }
            overlay(mapState)
        }

        // 3. Stroke を最上層に描画（境界線）
        Canvas(modifier = Modifier.matchParentSize()) {
            drawGeoJsonStroke(
                paths = paths,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
            )
        }
    }
}

/**
 * Draw region fill on the canvas
 */
private fun DrawScope.drawGeoJsonFill(
    paths: List<androidx.compose.ui.graphics.Path>,
    fillColor: Color,
) {
    paths.forEach { path ->
        drawPath(
            path = path,
            color = fillColor,
        )
    }
}

/**
 * Draw region stroke on the canvas
 */
private fun DrawScope.drawGeoJsonStroke(
    paths: List<androidx.compose.ui.graphics.Path>,
    strokeColor: Color,
    strokeWidth: Float,
) {
    paths.forEach { path ->
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
