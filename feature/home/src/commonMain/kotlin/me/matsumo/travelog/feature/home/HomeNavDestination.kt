package me.matsumo.travelog.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.ui.graphics.vector.ImageVector
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.home_navigation_map
import me.matsumo.travelog.core.resource.home_navigation_photos
import org.jetbrains.compose.resources.StringResource

internal data class HomeNavDestination(
    val label: StringResource,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val route: HomeRoute,
) {
    companion object Companion {
        val all = listOf(
            HomeNavDestination(
                label = Res.string.home_navigation_map,
                icon = Icons.Outlined.Map,
                iconSelected = Icons.Filled.Map,
                route = HomeRoute.Maps,
            ),
            HomeNavDestination(
                label = Res.string.home_navigation_photos,
                icon = Icons.Outlined.Photo,
                iconSelected = Icons.Filled.Photo,
                route = HomeRoute.Photos,
            )
        )
    }
}