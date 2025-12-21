package me.matsumo.travelog.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.matsumo.travelog.core.model.Destination
import me.matsumo.travelog.core.ui.component.GeoCanvasMap
import me.matsumo.travelog.core.ui.theme.LocalNavController
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val navController = LocalNavController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        uiState.geoJsonData?.let {
            GeoCanvasMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                geoJsonData = it,
            )
        }

        Button(
            onClick = { navController.navigate(Destination.Setting.Root) },
        ) {
            Text("Setting")
        }
    }
}
