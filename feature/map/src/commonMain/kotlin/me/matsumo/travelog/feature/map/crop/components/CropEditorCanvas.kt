package me.matsumo.travelog.feature.map.crop.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val VIEWPORT_PADDING = 0.1f

/**
 * Canvas for editing crop transform with gesture support using Zoomable library.
 *
 * - Drag to adjust position with inertia
 * - Pinch to zoom smoothly
 * - Shows polygon outline at all times (fixed position)
 * - Shows semi-transparent mask outside polygon when idle (fixed position)
 *
 * Coordinate system:
 * - Zoomable operates with ContentScale.Fit
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
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val zoomState = rememberZoomState(maxScale = 5f)
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var isIdle by remember { mutableStateOf(true) }

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
    LaunchedEffect(zoomState) {
        snapshotFlow {
            Triple(zoomState.scale, zoomState.offsetX, zoomState.offsetY)
        }
            .drop(1) // 初回値をスキップ
            .distinctUntilChanged()
            .conflate() // 中間値を破棄
            .onEach { isIdle = false }
            .debounce(50) // 動作停止後すぐに idle に戻す
            .collect { isIdle = true }
    }

    // ViewModel へトランスフォームを通知
    LaunchedEffect(zoomState, containerSize, imageSize) {
        snapshotFlow {
            TransformSnapshot(
                scale = zoomState.scale,
                offsetX = zoomState.offsetX,
                offsetY = zoomState.offsetY,
                containerSize = containerSize,
                imageSize = imageSize,
            )
        }
            .filter { it.containerSize.width > 0 && it.containerSize.height > 0 && it.imageSize.width > 0 && it.imageSize.height > 0 }
            .distinctUntilChanged()
            .collect { snapshot ->
                val fitScale = calculateFitScale(snapshot.containerSize, snapshot.imageSize)
                val cropScale = calculateCropScale(snapshot.containerSize, snapshot.imageSize)
                val cropRatio = if (fitScale > 0f) cropScale / fitScale else 1f
                val cropScaleValue = if (cropRatio > 0f) snapshot.scale / cropRatio else snapshot.scale
                val offsetXNormalized = snapshot.offsetX / snapshot.containerSize.width.toFloat()
                val offsetYNormalized = snapshot.offsetY / snapshot.containerSize.height.toFloat()

                if (isIdle) {
                    Napier.d {
                        "CropEditorCanvas transform: scale=$cropScaleValue offset=($offsetXNormalized,$offsetYNormalized) " +
                                "view=${snapshot.containerSize.width}x${snapshot.containerSize.height}"
                    }
                }

                onTransformChanged(
                    cropScaleValue,
                    offsetXNormalized,
                    offsetYNormalized,
                    snapshot.containerSize.width.toFloat(),
                    snapshot.containerSize.height.toFloat(),
                    VIEWPORT_PADDING,
                )
            }
    }

    // ViewModel の状態から ZoomState を同期（初期値やボタン操作用）
    LaunchedEffect(cropTransform, containerSize, imageSize) {
        if (containerSize.width <= 0 || containerSize.height <= 0 || imageSize.width <= 0 || imageSize.height <= 0) {
            return@LaunchedEffect
        }

        val fitScale = calculateFitScale(containerSize, imageSize)
        val cropScale = calculateCropScale(containerSize, imageSize)
        val cropRatio = if (fitScale > 0f) cropScale / fitScale else 1f
        val targetScale = (cropTransform.scale * cropRatio).coerceIn(0.9f, 5f)
        val targetOffsetX = cropTransform.offsetX * containerSize.width
        val targetOffsetY = cropTransform.offsetY * containerSize.height

        val scaleDiff = abs(zoomState.scale - targetScale)
        val offsetDiff = max(abs(zoomState.offsetX - targetOffsetX), abs(zoomState.offsetY - targetOffsetY))
        if (scaleDiff < 0.001f && offsetDiff < 0.5f) {
            return@LaunchedEffect
        }

        val centerX = containerSize.width / 2f
        val centerY = containerSize.height / 2f
        val pivot = Offset(
            x = centerX - (targetOffsetX / targetScale),
            y = centerY - (targetOffsetY / targetScale),
        )
        Napier.d { "CropEditorCanvas sync zoomState: scale=$targetScale offset=($targetOffsetX,$targetOffsetY) pivot=$pivot" }
        zoomState.centerByLayoutCoordinate(
            offset = pivot,
            scale = targetScale,
            animationSpec = tween(0),
        )
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
                .zoomable(zoomState),
        ) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
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
                            zoomState.setContentSize(Size(width.toFloat(), height.toFloat()))
                            Napier.d { "CropEditorCanvas image size: ${width}x${height}" }
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

private data class TransformSnapshot(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val containerSize: IntSize,
    val imageSize: IntSize,
)

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
