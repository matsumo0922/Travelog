package me.matsumo.travelog.feature.map.photo.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoDetailTopAppBar(
    title: String,
    onBackClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
    isSaveEnabled: Boolean = true,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(
                onClick = onBackClicked,
                enabled = !isSaving,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = "Back",
                )
            }
        },
        actions = {
            IconButton(
                onClick = onDeleteClicked,
                enabled = !isSaving,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = "Delete",
                )
            }

            IconButton(
                onClick = onSaveClicked,
                enabled = !isSaving && isSaveEnabled,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = "Save",
                )
            }
        },
    )
}
