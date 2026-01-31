package me.matsumo.travelog.feature.map.components.moments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import me.matsumo.travelog.core.model.MomentItem

private const val STAGGER_DELAY_MS = 80

/**
 * Section container for displaying moments list.
 * Renders moment cards with stagger animation.
 */
@Composable
internal fun MomentsSection(
    moments: ImmutableList<MomentItem>,
    onMomentClick: (MomentItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        moments.forEachIndexed { index, moment ->
            MomentCard(
                moment = moment,
                onClick = { onMomentClick(moment) },
                animationDelay = index * STAGGER_DELAY_MS,
            )
        }
    }
}
