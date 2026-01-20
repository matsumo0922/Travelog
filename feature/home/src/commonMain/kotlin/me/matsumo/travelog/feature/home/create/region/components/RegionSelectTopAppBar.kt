package me.matsumo.travelog.feature.home.create.region.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.home_select_region
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RegionSelectTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MediumFlexibleTopAppBar(
        modifier = modifier,
        title = {
            Text(stringResource(Res.string.home_select_region))
        },
        navigationIcon = {
            IconButton(onBackClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = null,
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}
