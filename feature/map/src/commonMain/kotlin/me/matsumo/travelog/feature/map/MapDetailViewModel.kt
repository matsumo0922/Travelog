package me.matsumo.travelog.feature.map

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.MomentItem
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
import me.matsumo.travelog.core.usecase.GetMomentsForMapUseCase

class MapDetailViewModel(
    private val mapId: String,
    private val mapRepository: MapRepository,
    private val mapRegionRepository: MapRegionRepository,
    private val geoAreaRepository: GeoAreaRepository,
    private val getMapRegionImagesUseCase: GetMapRegionImagesUseCase,
    private val getMomentsForMapUseCase: GetMomentsForMapUseCase,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapDetailUiState>>(ScreenState.Loading())
    val screenState = _screenState.asStateFlow()

    init {
        observeMapRegions()
    }

    private fun observeMapRegions() {
        viewModelScope.launch {
            mapRegionRepository.observeMapRegionsByMapId(mapId)
                .collectLatest { regions ->
                    val currentState = _screenState.value
                    if (currentState is ScreenState.Idle) {
                        val (imageUrlMap, moments) = coroutineScope {
                            val imageUrlMapDeferred = async { getMapRegionImagesUseCase(regions) }
                            val momentsDeferred = async { getMomentsForMapUseCase(regions) }
                            imageUrlMapDeferred.await() to momentsDeferred.await()
                        }
                        _screenState.value = ScreenState.Idle(
                            currentState.data.copy(
                                regions = regions.toImmutableList(),
                                regionImageUrls = imageUrlMap.toImmutableMap(),
                                moments = moments.toImmutableList(),
                            ),
                        )
                    }
                }
        }
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                val (map, regions) = coroutineScope {
                    val mapDeferred = async { mapRepository.getMap(mapId) }
                    val regionsDeferred = async { mapRegionRepository.getMapRegionsByMapId(mapId) }

                    mapDeferred.await() to regionsDeferred.await()
                }

                val validMap = map ?: error("Map not found")

                val (geoArea, imageUrlMap, moments) = coroutineScope {
                    val geoAreaDeferred = async { geoAreaRepository.getAreaByIdWithChildren(validMap.rootGeoAreaId) }
                    val imageUrlMapDeferred = async { getMapRegionImagesUseCase(regions) }
                    val momentsDeferred = async { getMomentsForMapUseCase(regions) }

                    Triple(
                        geoAreaDeferred.await(),
                        imageUrlMapDeferred.await(),
                        momentsDeferred.await(),
                    )
                }

                val validGeoArea = geoArea ?: error("Geo area not found")

                MapDetailUiState(
                    map = validMap,
                    geoArea = validGeoArea,
                    regions = regions.toImmutableList(),
                    regionImageUrls = imageUrlMap.toImmutableMap(),
                    moments = moments.toImmutableList(),
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
    val moments: ImmutableList<MomentItem> = persistentListOf(),
)
