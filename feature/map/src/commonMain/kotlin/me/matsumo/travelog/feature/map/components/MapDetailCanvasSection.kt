package me.matsumo.travelog.feature.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.ui.component.ClippedRegionImage
import me.matsumo.travelog.core.ui.component.GeoCanvasMap
import me.matsumo.travelog.core.ui.component.GeoJsonRenderer

@Composable
internal fun MapDetailCanvasSection(
    geoArea: GeoArea,
    regions: ImmutableList<MapRegion>,
    regionImageUrls: ImmutableMap<String, String>,
    modifier: Modifier = Modifier,
) {
    // Map geoAreaId to MapRegion for quick lookup
    val regionMap = remember(regions) {
        regions.associateBy { it.geoAreaId }
    }

    // Calculate parent bounds from all children (e.g., all 47 prefectures)
    val parentBounds = remember(geoArea.children) {
        GeoJsonRenderer.calculateBounds(geoArea.children)
    }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Calculate parent transform for unified coordinate system
    val parentTransform = remember(parentBounds, containerSize) {
        if (parentBounds == null || containerSize.width == 0 || containerSize.height == 0) {
            null
        } else {
            GeoJsonRenderer.calculateViewportTransform(
                bounds = parentBounds,
                canvasWidth = containerSize.width.toFloat(),
                canvasHeight = containerSize.height.toFloat(),
            )
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .onSizeChanged { containerSize = it },
    ) {
        // Base map showing all regions with unified transform
        GeoCanvasMap(
            modifier = Modifier.fillMaxSize(),
            areas = geoArea.children.toImmutableList(),
            externalBounds = parentBounds,
            externalTransform = parentTransform,
        )

        // Overlay clipped images for regions with photos using unified transform
        geoArea.children.forEach { childArea ->
            val childAreaId = childArea.id ?: return@forEach
            val region = regionMap[childAreaId] ?: return@forEach
            val imageId = region.representativeImageId ?: return@forEach
            val imageUrl = regionImageUrls[imageId] ?: return@forEach

            ClippedRegionImage(
                modifier = Modifier.fillMaxSize(),
                imageUrl = imageUrl,
                geoArea = childArea,
                cropData = region.cropData,
                parentBounds = parentBounds,
                parentTransform = parentTransform,
            )
        }
    }
}
