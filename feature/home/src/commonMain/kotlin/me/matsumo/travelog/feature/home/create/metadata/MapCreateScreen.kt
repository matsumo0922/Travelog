package me.matsumo.travelog.feature.home.create.metadata

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.geo.GeoRegionGroup
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
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

@Suppress("UnusedParameter")
@Composable
private fun MapCreateScreen(
    region: SupportedRegion,
    group: GeoRegionGroup?,
    modifier: Modifier = Modifier,
) {
    // UI は今後実装
}
