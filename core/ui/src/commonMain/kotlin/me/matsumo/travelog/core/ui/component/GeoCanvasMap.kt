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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import me.matsumo.travelog.core.model.GeoJsonData
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * A Composable that displays GeoJSON polygon data with zoom and pan capabilities
 *
 * @param geoJsonData The GeoJSON data to display
 * @param modifier Modifier for the canvas
 * @param strokeColor Color for polygon borders
 * @param fillColor Color for polygon fill
 * @param strokeWidth Width of polygon borders
 */
@Composable
fun GeoCanvasMap(
    geoJsonData: GeoJsonData,
    modifier: Modifier = Modifier,
    strokeColor: Color = Color.Black,
    fillColor: Color = Color.Gray.copy(alpha = 0.3f),
    strokeWidth: Float = 0.1f,
) {
    val zoomState = rememberZoomState()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Pre-compute paths for better performance
    val paths by remember(geoJsonData, canvasSize) {
        derivedStateOf {
            if (canvasSize.width == 0 || canvasSize.height == 0) return@derivedStateOf emptyList()
            geoJsonData.features.flatMap { feature ->
                GeoJsonRenderer.createPath(
                    geometry = feature.geometry,
                    width = canvasSize.width.toFloat(),
                    height = canvasSize.height.toFloat(),
                ).map { path -> path }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .zoomable(zoomState),
    ) {
        translate(zoomState.offsetX, zoomState.offsetY) {
            scale(zoomState.scale) {
                drawGeoJson(
                    paths = paths,
                    strokeColor = strokeColor,
                    fillColor = fillColor,
                    strokeWidth = strokeWidth,
                )
            }
        }
    }
}

/**
 * Draw GeoJSON paths on the canvas
 */
private fun DrawScope.drawGeoJson(
    paths: List<androidx.compose.ui.graphics.Path>,
    strokeColor: Color,
    fillColor: Color,
    strokeWidth: Float,
) {
    paths.forEach { path ->
        // Draw filled polygon
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
