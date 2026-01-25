package me.matsumo.travelog.feature.map.crop

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.db.CropData
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.MapRegionRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState

class PhotoCropEditorViewModel(
    private val mapId: String,
    private val geoAreaId: String,
    private val imageId: String,
    private val imageUrl: String,
    private val existingRegionId: String?,
    private val geoAreaRepository: GeoAreaRepository,
    private val mapRegionRepository: MapRegionRepository,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<PhotoCropEditorUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<PhotoCropEditorUiState>> = _screenState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                val geoArea = geoAreaRepository.getAreaByIdWithChildren(geoAreaId)
                    ?: throw IllegalStateException("GeoArea not found")

                val existingRegion = existingRegionId?.let { mapRegionRepository.getMapRegion(it) }
                val initialCropData = existingRegion?.cropData ?: CropData()

                PhotoCropEditorUiState(
                    geoArea = geoArea,
                    imageUrl = imageUrl,
                    cropTransform = CropTransformState(
                        scale = initialCropData.scale,
                        offsetX = initialCropData.offsetX,
                        offsetY = initialCropData.offsetY,
                    ),
                    existingRegion = existingRegion,
                )
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }

    fun updateTransform(scale: Float, offsetX: Float, offsetY: Float) {
        val currentState = _screenState.value
        if (currentState is ScreenState.Idle) {
            _screenState.value = ScreenState.Idle(
                currentState.data.copy(
                    cropTransform = CropTransformState(
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                    ),
                ),
            )
        }
    }

    fun save(onSuccess: () -> Unit) {
        val currentState = _screenState.value
        if (currentState !is ScreenState.Idle) return

        viewModelScope.launch {
            _saveState.value = SaveState.Saving

            val uiState = currentState.data
            val cropData = CropData(
                scale = uiState.cropTransform.scale,
                offsetX = uiState.cropTransform.offsetX,
                offsetY = uiState.cropTransform.offsetY,
            )

            suspendRunCatching {
                if (uiState.existingRegion != null) {
                    mapRegionRepository.updateMapRegion(
                        uiState.existingRegion.copy(
                            representativeImageId = imageUrl,
                            cropData = cropData,
                        ),
                    )
                } else {
                    mapRegionRepository.createMapRegion(
                        MapRegion(
                            mapId = mapId,
                            geoAreaId = geoAreaId,
                            representativeImageId = imageUrl,
                            cropData = cropData,
                        ),
                    )
                }
            }.fold(
                onSuccess = {
                    _saveState.value = SaveState.Success
                    onSuccess()
                },
                onFailure = {
                    _saveState.value = SaveState.Error
                },
            )
        }
    }
}

@Stable
data class PhotoCropEditorUiState(
    val geoArea: GeoArea,
    val imageUrl: String,
    val cropTransform: CropTransformState,
    val existingRegion: MapRegion?,
)

@Stable
data class CropTransformState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data object Success : SaveState
    data object Error : SaveState
}
