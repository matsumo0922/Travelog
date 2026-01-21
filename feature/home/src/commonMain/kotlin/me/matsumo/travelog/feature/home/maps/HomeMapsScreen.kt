package me.matsumo.travelog.feature.home.maps

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import me.matsumo.travelog.core.model.db.Map
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.home_map_add
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import me.matsumo.travelog.core.ui.screen.Destination
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.feature.home.maps.components.HomeMapsTopAppBar
import me.matsumo.travelog.feature.home.maps.components.MapItem
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeMapsScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeMapsViewModel = koinViewModel(),
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
        retryAction = viewModel::fetch,
    ) {
        IdleScreen(
            modifier = Modifier.fillMaxSize(),
            maps = it.maps.toImmutableList(),
            onDeleteMap = viewModel::deleteMap,
        )
    }
}

@Composable
private fun IdleScreen(
    maps: ImmutableList<Map>,
    onDeleteMap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current

    Scaffold(
        modifier = modifier,
        topBar = {
            HomeMapsTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                onSettingClicked = { navBackStack.add(Destination.Setting.Root) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navBackStack.add(Destination.CountrySelect) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Reviews",
                )

                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(Res.string.home_map_add),
                )
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = it
        ) {
            items(
                items = maps,
                key = { map -> map.id.orEmpty() },
            ) { map ->
                MapItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    map = map,
                    onLongClick = { map.id?.let(onDeleteMap) },
                )
            }
        }
    }
}
