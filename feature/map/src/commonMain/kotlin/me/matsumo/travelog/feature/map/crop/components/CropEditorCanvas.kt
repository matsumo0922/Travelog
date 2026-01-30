package me.matsumo.travelog.feature.map.crop.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.toUri
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.ui.component.GeoJsonRenderer
import me.matsumo.travelog.feature.map.crop.CropTransformState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val VIEWPORT_PADDING = 0.1f

/**
 * Multiplier for extending the panning range beyond the actual image bounds.
 * A value of 2.0 means the user can pan the image up to 100% of the image size
 * in any direction beyond its fitted position.
 */
private const val CONTENT_SIZE_MULTIPLIER = 2.0f

private const val MIN_SCALE = 0.9f
private const val MAX_SCALE = 5f

/**
 * Canvas for editing crop transform with gesture support.
 *
 * - Drag to adjust position
 * - Pinch to zoom smoothly
 * - Shows polygon outline at all times (fixed position)
 * - Shows semi-transparent mask outside polygon when idle (fixed position)
 *
 * Coordinate system:
 * - Transform operates with ContentScale.Fit basis
 * - DB stores Crop-based values
 * - cropRatio = cropBase / fitBase converts between them
 */
@OptIn(FlowPreview::class)
@Composable
internal fun CropEditorCanvas(
    localFilePath: String,
    geoArea: GeoArea,
    cropTransform: CropTransformState,
    onTransformChanged: (
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        viewWidth: Float,
        viewHeight: Float,
        viewportPadding: Float,
        rotation: Float,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current

    // Transform state (managed locally)
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }

    // Target rotation for animation (from ViewModel)
    var targetRotation by remember { mutableFloatStateOf(0f) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var isIdle by remember { mutableStateOf(true) }
    var isAnimating by remember { mutableStateOf(false) }
    var isGesturing by remember { mutableStateOf(false) }

    val areas = remember(geoArea) {
        geoArea.children.takeIf { it.isNotEmpty() }?.toImmutableList()
            ?: listOf(geoArea).toImmutableList()
    }

    val bounds = remember(areas) {
        GeoJsonRenderer.calculateBounds(areas)
    }

    val viewportTransform = remember(bounds, containerSize) {
        if (bounds == null || containerSize.width == 0 || containerSize.height == 0) {
            null
        } else {
            GeoJsonRenderer.calculateViewportTransform(
                bounds = bounds,
                canvasWidth = containerSize.width.toFloat(),
                canvasHeight = containerSize.height.toFloat(),
                padding = VIEWPORT_PADDING,
            )
        }
    }

    val regionClipPath by remember(areas, bounds, viewportTransform) {
        derivedStateOf {
            if (bounds == null || viewportTransform == null) {
                return@derivedStateOf null
            }
            val paths = GeoJsonRenderer.createPaths(
                areas = areas,
                bounds = bounds,
                transform = viewportTransform,
            )
            Path().apply {
                paths.forEach { addPath(it) }
            }
        }
    }

    // ジェスチャー状態の検出（即座に反応）
    LaunchedEffect(Unit) {
        snapshotFlow {
            TransformValues(scale, offsetX, offsetY, rotation)
        }
            .drop(1)
            .distinctUntilChanged()
            .conflate()
            .onEach { isIdle = false }
            .debounce(50)
            .collect { isIdle = true }
    }

    // ViewModel へトランスフォームを通知（アニメーション中はスキップ）
    LaunchedEffect(Unit) {
        snapshotFlow {
            TransformSnapshot(
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                rotation = rotation,
                containerSize = containerSize,
                imageSize = imageSize,
            )
        }
            .filter {
                it.containerSize.width > 0 && it.containerSize.height > 0 && it.imageSize.width > 0 && it.imageSize.height > 0
            }
            .distinctUntilChanged()
            .collect { snapshot ->
                // Skip notification during animation to prevent feedback loop
                if (isAnimating) return@collect

                val fitScale = calculateFitScale(snapshot.containerSize, snapshot.imageSize)
                val cropScale = calculateCropScale(snapshot.containerSize, snapshot.imageSize)
                val cropRatio = if (fitScale > 0f) cropScale / fitScale else 1f
                val cropScaleValue = if (cropRatio > 0f) snapshot.scale / cropRatio else snapshot.scale
                val offsetXNormalized = snapshot.offsetX / snapshot.containerSize.width.toFloat()
                val offsetYNormalized = snapshot.offsetY / snapshot.containerSize.height.toFloat()

                if (isIdle) {
                    Napier.d {
                        "CropEditorCanvas transform: scale=$cropScaleValue offset=($offsetXNormalized,$offsetYNormalized) " +
                            "rotation=${snapshot.rotation} view=${snapshot.containerSize.width}x${snapshot.containerSize.height}"
                    }
                }

                onTransformChanged(
                    cropScaleValue,
                    offsetXNormalized,
                    offsetYNormalized,
                    snapshot.containerSize.width.toFloat(),
                    snapshot.containerSize.height.toFloat(),
                    VIEWPORT_PADDING,
                    snapshot.rotation,
                )
            }
    }

    // ViewModel の状態から Transform を同期（初期値やボタン操作用）
    // ジェスチャー中はスキップして干渉を防ぐ
    LaunchedEffect(cropTransform, containerSize, imageSize) {
        if (containerSize.width <= 0 || containerSize.height <= 0 || imageSize.width <= 0 || imageSize.height <= 0) {
            return@LaunchedEffect
        }
        if (isGesturing) {
            return@LaunchedEffect
        }

        val fitScale = calculateFitScale(containerSize, imageSize)
        val cropScale = calculateCropScale(containerSize, imageSize)
        val cropRatio = if (fitScale > 0f) cropScale / fitScale else 1f
        val targetScaleValue = (cropTransform.scale * cropRatio).coerceIn(MIN_SCALE, MAX_SCALE)
        val targetOffsetX = cropTransform.offsetX * containerSize.width
        val targetOffsetY = cropTransform.offsetY * containerSize.height
        val targetRotationValue = normalizeRotationTarget(rotation, cropTransform.rotation)

        val scaleDiff = abs(scale - targetScaleValue)
        val offsetDiff = max(abs(offsetX - targetOffsetX), abs(offsetY - targetOffsetY))
        val rotationDiff = abs(rotation - targetRotationValue)
        if (scaleDiff < 0.001f && offsetDiff < 0.5f && rotationDiff < 0.1f) {
            return@LaunchedEffect
        }

        Napier.d {
            "CropEditorCanvas sync transform: scale=$targetScaleValue offset=($targetOffsetX,$targetOffsetY) rotation=$targetRotationValue"
        }

        // Update target rotation for animation
        targetRotation = targetRotationValue

        // Capture start values for animation
        val startScale = scale
        val startOffsetX = offsetX
        val startOffsetY = offsetY
        val startRotation = rotation

        // Animate using progress-based interpolation
        isAnimating = true
        try {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ) { progress, _ ->
                scale = startScale + (targetScaleValue - startScale) * progress
                offsetX = startOffsetX + (targetOffsetX - startOffsetX) * progress
                offsetY = startOffsetY + (targetOffsetY - startOffsetY) * progress
                rotation = startRotation + (targetRotationValue - startRotation) * progress
            }
        } finally {
            isAnimating = false
            // Normalize rotation to 0-360 range after animation
            rotation = normalizeRotation(rotation)
            // Notify ViewModel with final values after animation completes
            val fitScaleFinal = calculateFitScale(containerSize, imageSize)
            val cropScaleFinal = calculateCropScale(containerSize, imageSize)
            val cropRatioFinal = if (fitScaleFinal > 0f) cropScaleFinal / fitScaleFinal else 1f
            val cropScaleValueFinal = if (cropRatioFinal > 0f) scale / cropRatioFinal else scale
            onTransformChanged(
                cropScaleValueFinal,
                offsetX / containerSize.width.toFloat(),
                offsetY / containerSize.height.toFloat(),
                containerSize.width.toFloat(),
                containerSize.height.toFloat(),
                VIEWPORT_PADDING,
                normalizeRotation(rotation),
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                if (size != containerSize) {
                    containerSize = size
                    Napier.d { "CropEditorCanvas container size: ${size.width}x${size.height}" }
                }
            },
    ) {
        // 画像用のコンテナ（ズーム可能）- 最下層
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(containerSize, imageSize) {
                    if (containerSize.width <= 0 || containerSize.height <= 0 ||
                        imageSize.width <= 0 || imageSize.height <= 0
                    ) {
                        return@pointerInput
                    }

                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        isGesturing = true

                        try {
                            do {
                                val event = awaitPointerEvent()
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid()
                                // calculateRotation() returns degrees (not radians)
                                val rotationChange = event.calculateRotation()

                                if (zoomChange != 1f || panChange != Offset.Zero || rotationChange != 0f) {
                                    // Calculate new scale
                                    val newScale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)

                                    // Convert centroid from container coordinates to center-relative coordinates
                                    // (graphicsLayer uses center as origin for translation)
                                    val pivotX = centroid.x - containerSize.width / 2f
                                    val pivotY = centroid.y - containerSize.height / 2f

                                    // Calculate new offset with zoom pivot at pinch center
                                    // This formula keeps the point under the pinch center fixed during zoom
                                    val effectiveZoom = newScale / scale
                                    val newOffsetX = offsetX * effectiveZoom + pivotX * (1 - effectiveZoom) + panChange.x
                                    val newOffsetY = offsetY * effectiveZoom + pivotY * (1 - effectiveZoom) + panChange.y

                                    // Calculate new rotation (rotationChange is already in degrees)
                                    val newRotation = normalizeRotation(rotation + rotationChange)

                                    // Calculate extended bounds
                                    val fitScale = calculateFitScale(containerSize, imageSize)
                                    val fittedWidth = imageSize.width * fitScale
                                    val fittedHeight = imageSize.height * fitScale
                                    val scaledWidth = fittedWidth * newScale
                                    val scaledHeight = fittedHeight * newScale

                                    // Extended bounds: allow panning beyond the fitted image
                                    val extraRangeX = containerSize.width * (CONTENT_SIZE_MULTIPLIER - 1f) * 0.5f
                                    val extraRangeY = containerSize.height * (CONTENT_SIZE_MULTIPLIER - 1f) * 0.5f
                                    val boundX = max((scaledWidth - containerSize.width) * 0.5f, 0f) + extraRangeX
                                    val boundY = max((scaledHeight - containerSize.height) * 0.5f, 0f) + extraRangeY

                                    // Apply bounded offset and rotation (immediate update for gestures)
                                    scale = newScale
                                    offsetX = newOffsetX.coerceIn(-boundX, boundX)
                                    offsetY = newOffsetY.coerceIn(-boundY, boundY)
                                    rotation = newRotation

                                    // Consume the gesture
                                    event.changes.forEach { change ->
                                        if (change.positionChanged()) {
                                            change.consume()
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        } finally {
                            isGesturing = false
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                        rotationZ = rotation
                    },
                model = ImageRequest.Builder(context)
                    .data("file://$localFilePath".toUri())
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onSuccess = { state ->
                    val width = state.result.image.width
                    val height = state.result.image.height
                    if (width > 0 && height > 0) {
                        val newSize = IntSize(width, height)
                        if (newSize != imageSize) {
                            imageSize = newSize
                            Napier.d { "CropEditorCanvas image size: ${width}x$height" }
                        }
                    }
                },
            )
        }

        // マスクレイヤー（固定位置、アイドル時のみ表示）
        AnimatedVisibility(
            visible = isIdle,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(100)),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                regionClipPath?.let { path ->
                    clipPath(path, clipOp = ClipOp.Difference) {
                        drawRect(Color.Black.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // 輪郭線レイヤー（固定位置、常に表示）
        Canvas(modifier = Modifier.fillMaxSize()) {
            regionClipPath?.let { path ->
                drawPath(
                    path = path,
                    color = Color(0xFF33691E),
                    style = Stroke(
                        width = 2f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}

private data class TransformValues(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val rotation: Float,
)

private data class TransformSnapshot(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val rotation: Float,
    val containerSize: IntSize,
    val imageSize: IntSize,
)

/**
 * Normalizes rotation to 0-360 range.
 */
private fun normalizeRotation(rotation: Float): Float {
    val normalized = rotation % 360f
    return if (normalized < 0) normalized + 360f else normalized
}

/**
 * Calculates the target rotation using the shortest path.
 * This ensures 270° → 0° transitions as -90° (shortest path).
 */
private fun normalizeRotationTarget(current: Float, target: Float): Float {
    var diff = (target - current) % 360f
    if (diff > 180f) diff -= 360f
    if (diff < -180f) diff += 360f
    return current + diff
}

private fun calculateFitScale(containerSize: IntSize, imageSize: IntSize): Float {
    if (containerSize.width <= 0 || containerSize.height <= 0 || imageSize.width <= 0 || imageSize.height <= 0) {
        return 1f
    }
    val scaleX = containerSize.width.toFloat() / imageSize.width.toFloat()
    val scaleY = containerSize.height.toFloat() / imageSize.height.toFloat()
    return min(scaleX, scaleY)
}

private fun calculateCropScale(containerSize: IntSize, imageSize: IntSize): Float {
    if (containerSize.width <= 0 || containerSize.height <= 0 || imageSize.width <= 0 || imageSize.height <= 0) {
        return 1f
    }
    val scaleX = containerSize.width.toFloat() / imageSize.width.toFloat()
    val scaleY = containerSize.height.toFloat() / imageSize.height.toFloat()
    return max(scaleX, scaleY)
}
