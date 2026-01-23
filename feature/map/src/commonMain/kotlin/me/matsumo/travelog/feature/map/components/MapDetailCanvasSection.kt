package me.matsumo.travelog.feature.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.ui.component.GeoCanvasMap

@Composable
internal fun MapDetailCanvasSection(
    geoArea: GeoArea,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        GeoCanvasMap(
            modifier = Modifier.fillMaxSize(),
            areas = geoArea.children.toImmutableList(),
        )
    }
}
