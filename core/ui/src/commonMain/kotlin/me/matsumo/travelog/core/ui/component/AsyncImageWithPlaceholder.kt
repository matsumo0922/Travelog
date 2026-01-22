package me.matsumo.travelog.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.revenuecat.placeholder.PlaceholderDefaults
import com.revenuecat.placeholder.placeholder

@Composable
fun AsyncImageWithPlaceholder(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    var isLoading by rememberSaveable { mutableStateOf(true) }

    AsyncImage(
        modifier = modifier.placeholder(
            enabled = isLoading,
            highlight = PlaceholderDefaults.pulse,
        ),
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        onSuccess = {
            isLoading = false
        },
    )
}
