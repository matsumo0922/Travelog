package me.matsumo.travelog.feature.map.area.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.map_area_selected_count
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapAreaDetailTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onBackClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onCloseSelectionMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            if (isSelectionMode) {
                Text(text = stringResource(Res.string.map_area_selected_count, selectedCount))
            }
        },
        navigationIcon = {
            if (isSelectionMode) {
                IconButton(onClick = onCloseSelectionMode) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close selection mode",
                    )
                }
            } else {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
        actions = {
            if (isSelectionMode) {
                IconButton(onClick = onDeleteClicked) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}
