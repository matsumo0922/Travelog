package me.matsumo.travelog.feature.map.components.moments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import me.matsumo.travelog.core.model.MomentItem

// M3 Expressive emphasized decelerate easing
private val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private const val ANIMATION_DURATION_MS = 400

/**
 * Individual moment card component similar to Google Photos Moments.
 * Displays header (title + metadata) and photo grid with stagger animation.
 */
@Composable
internal fun MomentCard(
    moment: MomentItem,
    onClick: () -> Unit,
    animationDelay: Int,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION_MS,
                easing = EmphasizedDecelerateEasing,
            ),
        ) + slideInVertically(
            initialOffsetY = { 20 },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION_MS,
                easing = EmphasizedDecelerateEasing,
            ),
        ),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: Title + Metadata
            MomentHeader(
                geoArea = moment.geoArea,
                dateRange = moment.dateRange,
                imageCount = moment.totalImageCount,
            )

            // Photo Grid
            MomentPhotoGrid(
                previewImages = moment.previewImages.toImmutableList(),
                totalCount = moment.totalImageCount,
                onClick = onClick,
            )
        }
    }
}
