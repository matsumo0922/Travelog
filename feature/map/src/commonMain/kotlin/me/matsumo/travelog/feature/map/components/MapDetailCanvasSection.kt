package me.matsumo.travelog.feature.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.ui.component.ClippedRegionImage
import me.matsumo.travelog.core.ui.component.GeoCanvasMap

@Composable
internal fun MapDetailCanvasSection(
    geoArea: GeoArea,
    regions: ImmutableList<MapRegion>,
    modifier: Modifier = Modifier,
) {
    // Map geoAreaId to MapRegion for quick lookup
    val regionMap = remember(regions) {
        regions.associateBy { it.geoAreaId }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        // Base map showing all regions
        GeoCanvasMap(
            modifier = Modifier.fillMaxSize(),
            areas = geoArea.children.toImmutableList(),
        )

        // Overlay clipped images for regions with photos
        geoArea.children.forEach { childArea ->
            val childAreaId = childArea.id ?: return@forEach
            val region = regionMap[childAreaId] ?: return@forEach
            val imageUrl = region.representativeImageId ?: return@forEach

            ClippedRegionImage(
                modifier = Modifier.fillMaxSize(),
                imageUrl = imageUrl,
                geoArea = childArea,
                cropData = region.cropData,
            )
        }
    }
}
