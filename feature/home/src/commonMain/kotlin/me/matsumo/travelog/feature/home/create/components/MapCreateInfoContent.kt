package me.matsumo.travelog.feature.home.create.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.home_map_create
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MapCreateInfoContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        Button(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(24.dp, 16.dp)
                .fillMaxWidth(),
            onClick = { },
            contentPadding = ButtonDefaults.MediumContentPadding
        ) {
            Text(stringResource(Res.string.home_map_create))
        }
    }
}