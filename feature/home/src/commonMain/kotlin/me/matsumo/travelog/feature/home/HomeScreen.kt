package me.matsumo.travelog.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import me.matsumo.travelog.feature.home.maps.HomeMapsScreen
import me.matsumo.travelog.feature.home.photos.HomePhotosScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }

    val saveableStateHolder: SaveableStateHolder = rememberSaveableStateHolder()

    if (currentIndex > 0) {
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = true,
            onBackCompleted = { currentIndex = 0 },
        )
    }

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            for ((index, destination) in HomeNavDestination.all.withIndex()) {
                item(
                    selected = currentIndex == index,
                    onClick = { currentIndex = index },
                    icon = {
                        Icon(
                            imageVector = if (currentIndex == index) destination.iconSelected else destination.icon,
                            contentDescription = stringResource(destination.label)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(destination.label),
                        )
                    }
                )
            }
        }
    ) {
        AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            targetState = currentIndex,
        ) { index ->
            saveableStateHolder.SaveableStateProvider(index) {
                when (HomeNavDestination.all[index].route) {
                    HomeRoute.Maps -> {
                        HomeMapsScreen(
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    HomeRoute.Photos -> {
                        HomePhotosScreen(
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
