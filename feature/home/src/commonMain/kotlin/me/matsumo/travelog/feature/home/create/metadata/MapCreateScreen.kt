package me.matsumo.travelog.feature.home.create.metadata

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.PlatformFile
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.core.ui.utils.plus
import me.matsumo.travelog.feature.home.create.metadata.components.MapCreateBottomBar
import me.matsumo.travelog.feature.home.create.metadata.components.MapCreateDialog
import me.matsumo.travelog.feature.home.create.metadata.components.MapCreateMetadataSection
import me.matsumo.travelog.feature.home.create.metadata.components.MapCreateSelectedAreaSection
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
    val navBackStack = LocalNavBackStack.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.navigateToHome) {
        viewModel.navigateToHome.collect {
            // Remove all screens except the first (Home)
            while (navBackStack.size > 1) {
                navBackStack.removeLastOrNull()
            }
        }
    }

    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
        retryAction = viewModel::fetch,
    ) {
        MapCreateScreen(
            modifier = Modifier.fillMaxSize(),
            region = it.region,
            selectedArea = it.selectedArea,
            title = it.title,
            description = it.description,
            iconFile = it.iconFile,
            dialogState = dialogState,
            onTitleChange = viewModel::updateTitle,
            onDescriptionChange = viewModel::updateDescription,
            onIconFileChange = viewModel::updateIconFile,
            onBackClicked = { navBackStack.removeLastOrNull() },
            onCreateClicked = viewModel::createMap,
            onDialogRetry = viewModel::createMap,
            onDialogCancel = viewModel::dismissDialog,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapCreateScreen(
    region: SupportedRegion,
    selectedArea: GeoArea,
    title: String,
    description: String,
    iconFile: PlatformFile?,
    dialogState: MapCreateDialogState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onIconFileChange: (PlatformFile?) -> Unit,
    onBackClicked: () -> Unit,
    onCreateClicked: () -> Unit,
    onDialogRetry: () -> Unit,
    onDialogCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    MapCreateDialog(
        dialogState = dialogState,
        onRetry = onDialogRetry,
        onCancel = onDialogCancel,
    )

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MapCreateTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                scrollBehavior = scrollBehavior,
                onBackClicked = onBackClicked,
            )
        },
        bottomBar = {
            MapCreateBottomBar(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCreateClicked,
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding + PaddingValues(16.dp),
        ) {
            item {
                MapCreateSelectedAreaSection(
                    modifier = Modifier.fillMaxWidth(),
                    selectedArea = selectedArea,
                )
            }

            item {
                MapCreateMetadataSection(
                    modifier = Modifier.fillMaxWidth(),
                    title = title,
                    description = description,
                    iconFile = iconFile,
                    onTitleChange = onTitleChange,
                    onDescriptionChange = onDescriptionChange,
                    onIconFileChange = onIconFileChange,
                )
            }
        }
    }
}
