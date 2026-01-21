package me.matsumo.travelog.feature.home.create.metadata.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.home_map_create
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MapCreateBottomBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp)
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
        ) {
            Text(
                text = stringResource(Res.string.home_map_create),
            )
        }
    }
}
