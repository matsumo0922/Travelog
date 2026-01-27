package me.matsumo.travelog.feature.map.crop.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
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
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.ui.component.GeoJsonRenderer
import me.matsumo.travelog.feature.map.crop.CropTransformState
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * Canvas for editing crop transform with gesture support using Zoomable library.
 *
 * - Drag to adjust position with inertia
 * - Pinch to zoom smoothly
 * - Shows polygon outline at all times (fixed position)
 * - Shows semi-transparent mask outside polygon when idle (fixed position)
 */
@OptIn(FlowPreview::class)
@Composable
internal fun CropEditorCanvas(
    localFilePath: String,
    geoArea: GeoArea,
    initialTransform: CropTransformState,
    onTransformChanged: (scale: Float, offsetX: Float, offsetY: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val zoomState = rememberZoomState(maxScale = 5f)
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var contentSize by remember { mutableStateOf(Size.Zero) }
    var isIdle by remember { mutableStateOf(true) }
    var isInitialized by remember { mutableStateOf(false) }

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
                padding = 0.1f,
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

    // Initialize ZoomState from initialTransform when sizes are ready
    LaunchedEffect(containerSize, contentSize, initialTransform) {
        if (containerSize.width == 0 || contentSize.width == 0f || isInitialized) return@LaunchedEffect

        val layoutW = containerSize.width.toFloat()
        val layoutH = containerSize.height.toFloat()
        val contentW = contentSize.width
        val contentH = contentSize.height

        // ContentScale.Crop の baseScale
        val baseScale = maxOf(layoutW / contentW, layoutH / contentH)

        // CropTransformState → コンテンツ座標に変換
        // offsetX > 0 → コンテンツが右に動く → コンテンツの左側を中心にすべき
        val offsetPxX = initialTransform.offsetX * layoutW
        val offsetPxY = initialTransform.offsetY * layoutH
        val targetContentX = contentW / 2 - offsetPxX / (initialTransform.scale * baseScale)
        val targetContentY = contentH / 2 - offsetPxY / (initialTransform.scale * baseScale)

        zoomState.centerByContentCoordinate(
            offset = Offset(targetContentX, targetContentY),
            scale = initialTransform.scale,
            animationSpec = snap(),
        )
        isInitialized = true
    }

    // ZoomState → CropTransformState 変換（間引き付き）
    LaunchedEffect(zoomState, containerSize, contentSize) {
        if (containerSize.width == 0 || contentSize.width == 0f) return@LaunchedEffect

        val layoutW = containerSize.width.toFloat()
        val layoutH = containerSize.height.toFloat()
        val baseScale = maxOf(layoutW / contentSize.width, layoutH / contentSize.height)

        snapshotFlow {
            Triple(zoomState.scale, zoomState.offsetX, zoomState.offsetY)
        }
            .distinctUntilChanged()
            .sample(32) // 約30fps で間引き
            .collect { (scale, offsetX, offsetY) ->
                // ZoomState の offset → 正規化値に変換
                val normalizedOffsetX = -offsetX * scale * baseScale / layoutW
                val normalizedOffsetY = -offsetY * scale * baseScale / layoutH
                onTransformChanged(scale, normalizedOffsetX, normalizedOffsetY)
            }
    }

    // ジェスチャー状態の検出
    LaunchedEffect(zoomState) {
        snapshotFlow {
            Triple(zoomState.scale, zoomState.offsetX, zoomState.offsetY)
        }
            .drop(1) // 初回値をスキップ
            .distinctUntilChanged()
            .conflate() // 中間値を破棄
            .onEach { isIdle = false }
            .debounce(300)
            .collect { isIdle = true }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it },
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
                    contentSize = Size(
                        state.painter.intrinsicSize.width,
                        state.painter.intrinsicSize.height,
                    )
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
