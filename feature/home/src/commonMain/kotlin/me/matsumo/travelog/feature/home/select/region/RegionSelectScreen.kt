package me.matsumo.travelog.feature.home.select.region

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.geo.GeoRegionGroup
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun RegionSelectRoute(
    selectedCountryCode3: String,
    modifier: Modifier = Modifier,
    viewModel: RegionSelectViewModel = koinViewModel {
        parametersOf(selectedCountryCode3)
    }
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
    ) {
        RegionSelectScreen(
            modifier = Modifier.fillMaxSize(),
            region = it.region,
            groups = it.groups,
        )
    }
}

@Composable
private fun RegionSelectScreen(
    region: SupportedRegion,
    groups: ImmutableList<GeoRegionGroup>,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {

        }
    ) {

    }
}