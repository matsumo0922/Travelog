package me.matsumo.travelog.feature.map.select

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState

class MapSelectRegionViewModel(
    private val mapId: String,
    private val geoAreaId: String,
    private val geoAreaRepository: GeoAreaRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapSelectRegionUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<MapSelectRegionUiState>> = _screenState.asStateFlow()

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                val geoArea = geoAreaRepository.getAreaByIdWithChildren(geoAreaId)!!
                val sortedChildren = withContext(ioDispatcher) {
                    geoArea.children.sortedWith(
                        compareBy(
                            { it.isoCode == null },
                            { it.isoCode?.substringAfter("-")?.toIntOrNull() ?: Int.MAX_VALUE },
                            { it.name },
                        ),
                    )
                }

                MapSelectRegionUiState(
                    mapId = mapId,
                    geoArea = geoArea,
                    sortedChildren = sortedChildren.toImmutableList(),
                )
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }
}

@Stable
data class MapSelectRegionUiState(
    val mapId: String,
    val geoArea: GeoArea,
    val sortedChildren: ImmutableList<GeoArea>,
)
