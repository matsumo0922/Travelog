package me.matsumo.travelog.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.matsumo.travelog.core.model.Destination
import me.matsumo.travelog.core.ui.theme.LocalNavController
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val navController = LocalNavController.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = { navController.navigate(Destination.Setting.Root) },
        ) {
            Text("Setting")
        }
    }
}
