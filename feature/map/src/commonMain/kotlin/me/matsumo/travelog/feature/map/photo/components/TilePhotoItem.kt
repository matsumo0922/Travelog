package me.matsumo.travelog.feature.map.photo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import me.matsumo.travelog.core.ui.component.AsyncImageWithPlaceholder

@Composable
internal fun TilePhotoItem(
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    AsyncImageWithPlaceholder(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
    )
}
