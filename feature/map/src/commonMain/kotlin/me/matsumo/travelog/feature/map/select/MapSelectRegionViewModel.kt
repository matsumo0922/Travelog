package me.matsumo.travelog.feature.map.select

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.ui.screen.ScreenState

class MapSelectRegionViewModel(
    private val mapId: String,
    private val geoArea: GeoArea,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapSelectRegionUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<MapSelectRegionUiState>> = _screenState.asStateFlow()

    init {
        fetch()
    }

    fun fetch() {
        val sortedChildren = geoArea.children.sortedWith(
            compareBy(
                { it.isoCode == null },
                { it.isoCode?.substringAfter("-")?.toIntOrNull() ?: Int.MAX_VALUE },
                { it.name },
            ),
        )

        _screenState.value = ScreenState.Idle(
            MapSelectRegionUiState(
                mapId = mapId,
                geoArea = geoArea,
                sortedChildren = sortedChildren.toImmutableList(),
            ),
        )
    }
}

@Stable
data class MapSelectRegionUiState(
    val mapId: String,
    val geoArea: GeoArea,
    val sortedChildren: ImmutableList<GeoArea>,
)
