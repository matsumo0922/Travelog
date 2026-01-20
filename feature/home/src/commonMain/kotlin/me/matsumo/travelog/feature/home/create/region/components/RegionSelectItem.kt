package me.matsumo.travelog.feature.home.create.region.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.aakira.napier.Napier
import me.matsumo.travelog.core.model.geo.GeoRegionGroup
import me.matsumo.travelog.core.ui.theme.semiBold

@Composable
internal fun RegionSelectItem(
    group: GeoRegionGroup,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isJapanese = Locale.current.language == "ja"

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh),
        onClick = onSelected,
    ) {
        Box {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3 / 2f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(group.thumbnailUrl)
                        .crossfade(true)
                        .httpHeaders(
                            NetworkHeaders.Builder()
                                .add("User-Agent", "Travelog/1.0 (Android; Compose Multiplatform)")
                                .add("Referer", "https://ja.wikipedia.org/")
                                .build(),
                        )
                        .build(),
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Error) {
                            Napier.d { "Error loading region thumbnail: ${state.result}" }
                        }
                    },
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )

                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = group.nameJa.takeIf { isJapanese } ?: group.name,
                        style = MaterialTheme.typography.titleMedium.semiBold(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = group.admISO,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
