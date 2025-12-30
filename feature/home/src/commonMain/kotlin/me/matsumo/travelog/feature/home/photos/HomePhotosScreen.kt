package me.matsumo.travelog.feature.home.photos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.matsumo.travelog.core.ui.component.GeoCanvasMap
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomePhotosScreen(
    modifier: Modifier = Modifier,
    viewModel: HomePhotosViewModel = koinViewModel(),
) {
    val regions by viewModel.regions.collectAsStateWithLifecycle()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        GeoCanvasMap(
            modifier = Modifier.fillMaxSize(),
            regions = regions,
        )
    }
}