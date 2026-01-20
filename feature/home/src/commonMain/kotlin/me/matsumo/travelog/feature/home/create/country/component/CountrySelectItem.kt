package me.matsumo.travelog.feature.home.create.country.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.unit_region
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CountrySelectItem(
    supportedRegion: SupportedRegion,
    onSelected: (SupportedRegion) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = { onSelected(supportedRegion) })
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            modifier = Modifier.size(48.dp),
            model = supportedRegion.flagUrl,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(supportedRegion.nameRes),
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = supportedRegion.subRegionCount.toString() + stringResource(Res.string.unit_region),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.AutoMirrored.Default.ArrowForwardIos,
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = null,
        )
    }
}
