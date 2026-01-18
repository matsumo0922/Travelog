package me.matsumo.travelog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.svg.SvgDecoder
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import me.matsumo.travelog.core.model.AppSetting
import me.matsumo.travelog.core.ui.screen.Destination
import me.matsumo.travelog.core.ui.theme.TravelogTheme

@Composable
internal fun TravelogApp(
    setting: AppSetting,
    initialDestination: Destination,
    modifier: Modifier = Modifier,
) {
    SetupCoil()

    TravelogTheme(setting) {
        AppNavHost(
            modifier = modifier,
            initialDestination = initialDestination,
        )
    }
}

@Composable
private fun SetupCoil() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
                addPlatformFileSupport()
            }
            .build()
    }
}
