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
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.common_allow
import me.matsumo.travelog.core.ui.component.GeoCanvasMap
import me.matsumo.travelog.core.ui.screen.Destination
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val navBackStack = LocalNavBackStack.current
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
            onClick = { navBackStack.add(Destination.Setting.Root) },
        ) {
            Text(stringResource(Res.string.common_allow))
        }
    }
}
