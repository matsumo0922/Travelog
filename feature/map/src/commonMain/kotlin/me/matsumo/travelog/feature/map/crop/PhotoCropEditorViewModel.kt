package me.matsumo.travelog.feature.map.crop

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
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
import me.matsumo.travelog.core.resource.error_temp_file_not_found
import me.matsumo.travelog.core.ui.screen.ScreenState
import me.matsumo.travelog.core.usecase.SaveMapRegionPhotoUseCase
import me.matsumo.travelog.core.usecase.TempFileStorage

class PhotoCropEditorViewModel(
    private val mapId: String,
    private val geoAreaId: String,
    private val localFilePath: String,
    private val existingRegionId: String?,
    private val geoAreaRepository: GeoAreaRepository,
    private val mapRegionRepository: MapRegionRepository,
    private val saveMapRegionPhotoUseCase: SaveMapRegionPhotoUseCase,
    private val tempFileStorage: TempFileStorage,
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
                // Verify temp file exists (may be deleted by OS cache cleanup or nav restore)
                tempFileStorage.loadFromTemp(localFilePath)
                    ?: throw TempFileNotFoundException("Temp file not found: $localFilePath")

                val geoArea = geoAreaRepository.getAreaByIdWithChildren(geoAreaId)
                    ?: error("GeoArea not found")

                val existingRegion = existingRegionId?.let { mapRegionRepository.getMapRegion(it) }
                val initialCropData = existingRegion?.cropData ?: CropData()

                PhotoCropEditorUiState(
                    geoArea = geoArea,
                    localFilePath = localFilePath,
                    isTempFileValid = true,
                    cropTransform = CropTransformState(
                        scale = initialCropData.scale,
                        offsetX = initialCropData.offsetX,
                        offsetY = initialCropData.offsetY,
                        viewWidth = initialCropData.viewWidth,
                        viewHeight = initialCropData.viewHeight,
                        viewportPadding = initialCropData.viewportPadding,
                    ),
                    existingRegion = existingRegion,
                )
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { e ->
                    when (e) {
                        is TempFileNotFoundException -> ScreenState.Error(Res.string.error_temp_file_not_found)
                        else -> ScreenState.Error(Res.string.error_network)
                    }
                },
            )
        }
    }

    class TempFileNotFoundException(message: String) : Exception(message)

    fun updateTransform(
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        viewWidth: Float,
        viewHeight: Float,
        viewportPadding: Float,
    ) {
        val currentState = _screenState.value
        if (currentState is ScreenState.Idle) {
            _screenState.value = ScreenState.Idle(
                currentState.data.copy(
                    cropTransform = CropTransformState(
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        viewWidth = viewWidth,
                        viewHeight = viewHeight,
                        viewportPadding = viewportPadding,
                    ),
                ),
            )
        }
    }

    fun save(onSuccess: () -> Unit) {
        val currentState = _screenState.value
        if (currentState !is ScreenState.Idle) return

        viewModelScope.launch {
            _saveState.value = SaveState.Uploading

            val uiState = currentState.data
            val cropData = CropData(
                scale = uiState.cropTransform.scale,
                offsetX = uiState.cropTransform.offsetX,
                offsetY = uiState.cropTransform.offsetY,
                viewWidth = uiState.cropTransform.viewWidth,
                viewHeight = uiState.cropTransform.viewHeight,
                viewportPadding = uiState.cropTransform.viewportPadding,
            )

            _saveState.value = SaveState.Saving

            val result = saveMapRegionPhotoUseCase(
                mapId = mapId,
                geoAreaId = geoAreaId,
                localFilePath = localFilePath,
                geoArea = uiState.geoArea,
                cropData = cropData,
                existingRegion = uiState.existingRegion,
            )

            when (result) {
                is SaveMapRegionPhotoUseCase.Result.Success -> {
                    _saveState.value = SaveState.Success
                    onSuccess()
                }

                is SaveMapRegionPhotoUseCase.Result.TempFileNotFound,
                is SaveMapRegionPhotoUseCase.Result.UserNotLoggedIn,
                is SaveMapRegionPhotoUseCase.Result.UploadFailed,
                is SaveMapRegionPhotoUseCase.Result.SaveFailed,
                    -> {
                    Napier.e { "Failed to save photo crop: $result" }
                    _saveState.value = SaveState.Error
                }
            }
        }
    }
}

@Stable
data class PhotoCropEditorUiState(
    val geoArea: GeoArea,
    val localFilePath: String,
    val isTempFileValid: Boolean,
    val cropTransform: CropTransformState,
    val existingRegion: MapRegion?,
)

@Stable
data class CropTransformState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val viewWidth: Float = 0f,
    val viewHeight: Float = 0f,
    val viewportPadding: Float = 0.1f,
)

sealed interface SaveState {
    data object Idle : SaveState
    data object Uploading : SaveState
    data object Saving : SaveState
    data object Success : SaveState
    data object Error : SaveState
}
