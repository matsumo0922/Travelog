package me.matsumo.travelog.feature.home.create.country

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.ui.screen.Destination
import me.matsumo.travelog.core.ui.theme.LocalNavBackStack
import me.matsumo.travelog.core.ui.utils.plus
import me.matsumo.travelog.feature.home.create.country.component.CountrySelectItem
import me.matsumo.travelog.feature.home.create.country.component.CountrySelectTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CountrySelectScreen(
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current
    val supportedRegions = remember { SupportedRegion.all }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CountrySelectTopAppBar(
                modifier = Modifier.fillMaxWidth(),
                onBackClicked = { navBackStack.removeLastOrNull() },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = contentPadding + PaddingValues(16.dp),
        ) {
            itemsIndexed(
                items = supportedRegions,
                key = { _, region -> region.code2 },
            ) { index, region ->
                val isSectionStart = index == 0
                val isSectionEnd = index == supportedRegions.lastIndex

                val shape = RoundedCornerShape(
                    topStart = if (isSectionStart) 16.dp else 0.dp,
                    topEnd = if (isSectionStart) 16.dp else 0.dp,
                    bottomStart = if (isSectionEnd) 16.dp else 0.dp,
                    bottomEnd = if (isSectionEnd) 16.dp else 0.dp,
                )

                CountrySelectItem(
                    modifier = Modifier
                        .clip(shape)
                        .fillMaxWidth(),
                    supportedRegion = region,
                    onSelected = { navBackStack.add(Destination.RegionSelect(it.code3)) },
                )
            }
        }
    }
}
