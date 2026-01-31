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
            .height(320.dp)
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
        // Row 1: Single image
        if (images.isNotEmpty()) {
            SingleImageCell(
                image = images[0],
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }

        // Row 2: Single image
        if (images.size > 1) {
            SingleImageCell(
                image = images[1],
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }

        // Row 3: Two images side by side (left: normal, right: with overlay)
        if (images.size > 2) {
            BottomRow(
                leftImage = images[2],
                rightImage = images.getOrNull(3),
                remainingCount = remainingCount,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SingleImageCell(
    image: PreviewImage,
    modifier: Modifier = Modifier,
) {
    AsyncImageWithPlaceholder(
        modifier = modifier.clip(MaterialTheme.shapes.large),
        model = image.url,
        contentScale = ContentScale.Crop,
        contentDescription = null,
    )
}

@Composable
private fun BottomRow(
    leftImage: PreviewImage,
    rightImage: PreviewImage?,
    remainingCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Left image (normal)
        AsyncImageWithPlaceholder(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.large),
            model = leftImage.url,
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )

        // Right image (with overlay if there are more photos)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.large),
        ) {
            AsyncImageWithPlaceholder(
                modifier = Modifier.fillMaxSize(),
                model = rightImage?.url ?: leftImage.url,
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )

            if (remainingCount > 0) {
                MorePhotosOverlay(
                    count = remainingCount,
                    modifier = Modifier.fillMaxSize(),
                )
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
