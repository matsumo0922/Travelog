package me.matsumo.travelog.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import coil3.compose.AsyncImage
import kotlinx.collections.immutable.toImmutableList
import me.matsumo.travelog.core.model.db.CropData
import me.matsumo.travelog.core.model.geo.GeoArea

/**
 * A Composable that displays an image clipped to a geographic region's polygon shape.
 *
 * @param imageUrl URL of the image to display
 * @param geoArea The geographic area whose polygon shape will be used for clipping
 * @param cropData Optional crop transform data (scale and offset)
 * @param modifier Modifier for the component
 */
@Composable
fun ClippedRegionImage(
    imageUrl: String,
    geoArea: GeoArea,
    modifier: Modifier = Modifier,
    cropData: CropData? = null,
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val areas = remember(geoArea) {
        geoArea.children.takeIf { it.isNotEmpty() }?.toImmutableList()
            ?: listOf(geoArea).toImmutableList()
    }

    val bounds = remember(areas) {
        GeoJsonRenderer.calculateBounds(areas)
    }

    val clipShape = remember(areas, bounds, containerSize) {
        if (bounds == null || containerSize.width == 0 || containerSize.height == 0) {
            null
        } else {
            val transform = GeoJsonRenderer.calculateViewportTransform(
                bounds = bounds,
                canvasWidth = containerSize.width.toFloat(),
                canvasHeight = containerSize.height.toFloat(),
            )
            val paths = GeoJsonRenderer.createPaths(
                areas = areas,
                bounds = bounds,
                transform = transform,
            )
            GeoAreaClipShape(paths)
        }
    }

    val scale = cropData?.scale ?: 1f
    val offsetX = cropData?.offsetX ?: 0f
    val offsetY = cropData?.offsetY ?: 0f

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .then(clipShape?.let { Modifier.clip(it) } ?: Modifier),
    ) {
        AsyncImage(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX * size.width
                    translationY = offsetY * size.height
                },
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
    }
}

/**
 * Custom Shape that clips to a geographic area path.
 */
private class GeoAreaClipShape(
    private val paths: List<Path>,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val combinedPath = Path().apply {
            paths.forEach { addPath(it) }
        }
        return Outline.Generic(combinedPath)
    }
}
