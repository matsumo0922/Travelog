package me.matsumo.travelog.feature.map

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.db.Map
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.MapRegionRepository
import me.matsumo.travelog.core.repository.MapRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState
import me.matsumo.travelog.core.usecase.GetMapRegionImagesUseCase

class MapDetailViewModel(
    private val mapId: String,
    private val mapRepository: MapRepository,
    private val mapRegionRepository: MapRegionRepository,
    private val geoAreaRepository: GeoAreaRepository,
    private val getMapRegionImagesUseCase: GetMapRegionImagesUseCase,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapDetailUiState>>(ScreenState.Loading())
    val screenState = _screenState.asStateFlow()

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                Napier.d { "fetch start." }

                // Phase 1: map と regions を並列取得
                val (map, regions) = coroutineScope {
                    val mapDeferred = async { mapRepository.getMap(mapId) }
                    val regionsDeferred = async { mapRegionRepository.getMapRegionsByMapId(mapId) }
                    mapDeferred.await() to regionsDeferred.await()
                }
                Napier.d { "1. fetch map and regions. map=${map?.title}, regions=${regions.size}" }

                val validMap = map ?: error("Map not found")

                // Phase 2: geoArea と imageUrlMap を並列取得
                val (geoArea, imageUrlMap) = coroutineScope {
                    val geoAreaDeferred = async {
                        geoAreaRepository.getAreaByIdWithChildren(validMap.rootGeoAreaId, true)
                    }
                    val imageUrlMapDeferred = async {
                        getMapRegionImagesUseCase(regions)
                    }
                    geoAreaDeferred.await() to imageUrlMapDeferred.await()
                }
                Napier.d { "2. fetch geoArea and imageUrls. area=${geoArea?.name}, urls=${imageUrlMap.size}. end." }

                val validGeoArea = geoArea ?: error("Geo area not found")

                MapDetailUiState(
                    map = validMap,
                    geoArea = validGeoArea,
                    regions = regions.toImmutableList(),
                    regionImageUrls = imageUrlMap.toImmutableMap(),
                )
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }
}

@Stable
data class MapDetailUiState(
    val map: Map,
    val geoArea: GeoArea,
    val regions: ImmutableList<MapRegion>,
    val regionImageUrls: ImmutableMap<String, String>,
)
