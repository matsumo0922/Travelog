package me.matsumo.travelog.feature.home.select

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import me.matsumo.travelog.core.ui.screen.AsyncLoadContents
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun CountrySelectRoute(
    modifier: Modifier = Modifier,
    viewModel: CountrySelectViewModel = koinViewModel(),
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
    ) {
        CountrySelectScreen(
            modifier = Modifier.fillMaxSize(),
            countryCodes = it.countryCodes.toImmutableList(),
        )
    }
}

@Composable
private fun CountrySelectScreen(
    countryCodes: ImmutableList<String>,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            items(countryCodes) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
