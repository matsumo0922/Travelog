package me.matsumo.travelog.feature.home.create.metadata

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.geo.GeoRegionGroup
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.core.ui.utils.plus
import me.matsumo.travelog.feature.home.create.metadata.components.MapCreateBottomBar
import me.matsumo.travelog.feature.home.create.metadata.components.MapCreateTopAppBar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun MapCreateRoute(
    selectedCountryCode3: String,
    selectedGroupAdmId: String?,
    modifier: Modifier = Modifier,
    viewModel: MapCreateViewModel = koinViewModel(
        key = selectedCountryCode3 + selectedGroupAdmId.orEmpty(),
    ) {
        parametersOf(selectedCountryCode3, selectedGroupAdmId)
    },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
        retryAction = viewModel::fetch,
    ) {
        MapCreateScreen(
            modifier = Modifier.fillMaxSize(),
            region = it.region,
            group = it.group,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UnusedParameter")
@Composable
private fun MapCreateScreen(
    region: SupportedRegion,
    group: GeoRegionGroup?,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MapCreateTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                scrollBehavior = scrollBehavior,
                onBackClicked = { },
            )
        },
        bottomBar = {
            MapCreateBottomBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(24.dp, 8.dp)
                    .fillMaxWidth(),
                onClick = { },
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding + PaddingValues(16.dp),
        ) {

        }
    }
}
