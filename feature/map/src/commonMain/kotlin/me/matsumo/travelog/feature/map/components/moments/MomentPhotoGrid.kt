package me.matsumo.travelog.feature.map.components.moments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import me.matsumo.travelog.core.model.PreviewImage
import me.matsumo.travelog.core.ui.component.AsyncImageWithPlaceholder
import me.matsumo.travelog.core.ui.theme.semiBold

/**
 * Photo grid layout similar to Google Photos Moments.
 * Displays "+X" overlay on the last visible image when there are more photos.
 */
@Composable
internal fun MomentPhotoGrid(
    previewImages: List<PreviewImage>,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    if (previewImages.isEmpty()) return

    val remainingCount = (totalCount - previewImages.size).coerceAtLeast(0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(MaterialTheme.shapes.large),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AsyncImageWithPlaceholder(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.large),
            model = previewImages.first().url,
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )

        if (previewImages.size > 1) {
            RightColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                images = previewImages.drop(1).take(3),
                remainingCount = remainingCount,
            )
        }
    }
}

@Composable
private fun RightColumn(
    images: List<PreviewImage>,
    remainingCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        images.forEachIndexed { index, image ->
            val isLastWithMore = index == images.lastIndex && remainingCount > 0

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large),
            ) {
                AsyncImageWithPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    model = image.url,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )

                if (isLastWithMore) {
                    MorePhotosOverlay(
                        count = remainingCount,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MorePhotosOverlay(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+$count",
            style = MaterialTheme.typography.titleLarge.semiBold(),
            color = Color.White,
        )
    }
}
