package me.matsumo.travelog.feature.home.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.home_map_create
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.feature.home.create.components.MapCreateSelectCountryContent
import me.matsumo.travelog.feature.home.create.components.MapCreateTopAppBar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MapCreateScreen(
    modifier: Modifier = Modifier,
    viewModel: MapCreateViewModel = koinViewModel()
) {
    val navBackStack = LocalNavBackStack.current
    var index by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        topBar = {
            MapCreateTopAppBar(
                modifier = Modifier,
                onBackClicked = { navBackStack.removeLastOrNull() },
            )
        },
        bottomBar = {
            Button(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(24.dp, 16.dp)
                    .fillMaxWidth(),
                onClick = { },
                contentPadding = ButtonDefaults.MediumContentPadding
            ) {
                Text(stringResource(Res.string.home_map_create))
            }
        },
    ) { padding ->
        AnimatedContent(
            modifier = Modifier.padding(padding),
            targetState = index,
        ) {
            when (it) {
                0 -> {
                    MapCreateSelectCountryContent(
                        modifier = Modifier.fillMaxSize(),
                        onCountrySelected = {
                            index = 1
                        }
                    )
                }
            }
        }
    }
}