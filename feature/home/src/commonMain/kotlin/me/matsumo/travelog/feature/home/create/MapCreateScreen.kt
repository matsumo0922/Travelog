package me.matsumo.travelog.feature.home.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.feature.home.create.components.MapCreateInfoContent
import me.matsumo.travelog.feature.home.create.components.MapCreateSelectCountryContent
import me.matsumo.travelog.feature.home.create.components.MapCreateSelectRegionContent
import me.matsumo.travelog.feature.home.create.components.MapCreateTopAppBar
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MapCreateScreen(
    modifier: Modifier = Modifier,
    viewModel: MapCreateViewModel = koinViewModel()
) {
    val navBackStack = LocalNavBackStack.current

    val selectRegionScreenState by viewModel.selectRegionScreenState.collectAsStateWithLifecycle()
    var selectedRegion by remember { mutableStateOf<SupportedRegion?>(null) }
    var index by rememberSaveable { mutableIntStateOf(0) }

    if (index > 0) {
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = true,
            onBackCompleted = { index = (index - 1).coerceAtLeast(0) },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            MapCreateTopAppBar(
                modifier = Modifier,
                onBackClicked = { navBackStack.removeLastOrNull() },
            )
        },
    ) { padding ->
        AnimatedContent(
            modifier = Modifier.padding(padding),
            targetState = index,
        ) { selectedIndex ->
            when (selectedIndex) {
                0 -> {
                    MapCreateSelectCountryContent(
                        modifier = Modifier.fillMaxSize(),
                        onCountrySelected = {
                            selectedRegion = it
                            viewModel.downloadRegion(it)
                            index = 1
                        }
                    )
                }

                1 -> {
                    MapCreateSelectRegionContent(
                        modifier = Modifier.fillMaxSize(),
                        screenState = selectRegionScreenState,
                        onRetryClicked = selectedRegion?.let { region ->
                            { viewModel.downloadRegion(region) }
                        }
                    )
                }

                2 -> {
                    MapCreateInfoContent(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}