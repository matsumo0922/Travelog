package me.matsumo.travelog.feature.home.maps

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeMapsScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeMapsViewModel = koinViewModel(),
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text("Maps")
    }
}