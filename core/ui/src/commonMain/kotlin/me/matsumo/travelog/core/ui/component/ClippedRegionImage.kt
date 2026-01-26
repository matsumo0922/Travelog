package me.matsumo.travelog.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalDensity
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
 * @param modifier Modifier for the component
 * @param cropData Optional crop transform data (scale and offset) - used for original images
 * @param isPreCropped Whether the image is pre-cropped (already has polygon clipping and cropData applied)
 * @param parentBounds Parent bounds to use (for coordinate system unification)
 * @param parentTransform Parent transform to use (for coordinate system unification)
 */
@Composable
fun ClippedRegionImage(
    imageUrl: String,
    geoArea: GeoArea,
    modifier: Modifier = Modifier,
    cropData: CropData? = null,
    isPreCropped: Boolean = false,
    parentBounds: GeoJsonRenderer.Bounds? = null,
    parentTransform: GeoJsonRenderer.ViewportTransform? = null,
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    val areas = remember(geoArea) {
        geoArea.children.takeIf { it.isNotEmpty() }?.toImmutableList()
            ?: listOf(geoArea).toImmutableList()
    }

    // Use parent bounds if provided, otherwise calculate from own areas
    val effectiveBounds = remember(areas, parentBounds) {
        parentBounds ?: GeoJsonRenderer.calculateBounds(areas)
    }

    // Calculate own bounds for pre-cropped image positioning
    val ownBounds = remember(areas) {
        GeoJsonRenderer.calculateBounds(areas)
    }

    val clipShape = remember(areas, effectiveBounds, containerSize, parentTransform) {
        // If parent transform is provided, container size check is not needed
        val hasValidSize = parentTransform != null || (containerSize.width > 0 && containerSize.height > 0)

        if (effectiveBounds == null || !hasValidSize) {
            null
        } else {
            val transform = parentTransform ?: GeoJsonRenderer.calculateViewportTransform(
                bounds = effectiveBounds,
                canvasWidth = containerSize.width.toFloat(),
                canvasHeight = containerSize.height.toFloat(),
            )
            val paths = GeoJsonRenderer.createPaths(
                areas = areas,
                bounds = effectiveBounds,
                transform = transform,
            )
            GeoAreaClipShape(paths)
        }
    }

    // Calculate the bounding box of the child area in parent coordinate system
    val childBoundingBox = remember(areas, effectiveBounds, containerSize, parentTransform, ownBounds) {
        if (effectiveBounds == null || ownBounds == null) return@remember null
        if (parentTransform == null && (containerSize.width <= 0 || containerSize.height <= 0)) return@remember null

        val transform = parentTransform ?: GeoJsonRenderer.calculateViewportTransform(
            bounds = effectiveBounds,
            canvasWidth = containerSize.width.toFloat(),
            canvasHeight = containerSize.height.toFloat(),
        )

        // Calculate the position of child's bounds in parent coordinate system
        val minX = transform.offsetX + ((ownBounds.minLon - effectiveBounds.minLon) * transform.scale).toFloat()
        val maxX = transform.offsetX + ((ownBounds.maxLon - effectiveBounds.minLon) * transform.scale).toFloat()

        val parentMaxLatMerc = latToMercator(effectiveBounds.maxLat)
        val childMinLatMerc = latToMercator(ownBounds.minLat)
        val childMaxLatMerc = latToMercator(ownBounds.maxLat)

        val minY = transform.offsetY + ((parentMaxLatMerc - childMaxLatMerc) * transform.scale).toFloat()
        val maxY = transform.offsetY + ((parentMaxLatMerc - childMinLatMerc) * transform.scale).toFloat()

        BoundingBox(
            x = minX,
            y = minY,
            width = maxX - minX,
            height = maxY - minY,
        )
    }

    val scale = cropData?.scale ?: 1f
    val offsetX = cropData?.offsetX ?: 0f
    val offsetY = cropData?.offsetY ?: 0f

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .then(clipShape?.let { Modifier.clip(it) } ?: Modifier),
    ) {
        if (isPreCropped && childBoundingBox != null) {
            // Pre-cropped image: position at child's bounding box
            // The image is 512x512 with polygon filling the bounds (no padding),
            // so we use FillBounds to stretch it to match childBoundingBox
            with(density) {
                AsyncImage(
                    modifier = Modifier
                        .offset(
                            x = childBoundingBox.x.toDp(),
                            y = childBoundingBox.y.toDp(),
                        )
                        .size(
                            width = childBoundingBox.width.toDp(),
                            height = childBoundingBox.height.toDp(),
                        ),
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                )
            }
        } else {
            // Original image: apply cropData transform
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
}

private data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

private fun latToMercator(lat: Double): Double {
    val latRad = lat * kotlin.math.PI / 180.0
    return kotlin.math.ln(kotlin.math.tan(kotlin.math.PI / 4.0 + latRad / 2.0)) * 180.0 / kotlin.math.PI
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
