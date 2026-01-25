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
import me.matsumo.travelog.core.repository.SessionRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.resource.error_temp_file_not_found
import me.matsumo.travelog.core.ui.screen.ScreenState
import me.matsumo.travelog.core.usecase.TempFileStorage
import me.matsumo.travelog.core.usecase.UploadMapIconUseCase

class PhotoCropEditorViewModel(
    private val mapId: String,
    private val geoAreaId: String,
    private val localFilePath: String,
    private val existingRegionId: String?,
    private val geoAreaRepository: GeoAreaRepository,
    private val mapRegionRepository: MapRegionRepository,
    private val sessionRepository: SessionRepository,
    private val uploadMapIconUseCase: UploadMapIconUseCase,
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
                val tempFile = tempFileStorage.loadFromTemp(localFilePath)
                    ?: throw TempFileNotFoundException("Temp file not found: $localFilePath")

                val geoArea = geoAreaRepository.getAreaByIdWithChildren(geoAreaId)
                    ?: throw IllegalStateException("GeoArea not found")

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
            _saveState.value = SaveState.Uploading

            val uiState = currentState.data
            val cropData = CropData(
                scale = uiState.cropTransform.scale,
                offsetX = uiState.cropTransform.offsetX,
                offsetY = uiState.cropTransform.offsetY,
            )

            suspendRunCatching {
                // 1. Load file from temp storage
                val file = tempFileStorage.loadFromTemp(localFilePath)
                    ?: throw IllegalStateException("Temp file not found: $localFilePath")

                // 2. Get current user ID
                val userId = sessionRepository.getCurrentUserInfo()?.id
                    ?: throw IllegalStateException("User not logged in")

                // 3. Upload image and get imageId
                val uploadResult = uploadMapIconUseCase(file, userId)
                val imageId = uploadResult.imageId

                // 4. Create or update MapRegion
                _saveState.value = SaveState.Saving

                if (uiState.existingRegion != null) {
                    mapRegionRepository.updateMapRegion(
                        uiState.existingRegion.copy(
                            representativeImageId = imageId,
                            cropData = cropData,
                        ),
                    )
                } else {
                    mapRegionRepository.createMapRegion(
                        MapRegion(
                            mapId = mapId,
                            geoAreaId = geoAreaId,
                            representativeImageId = imageId,
                            cropData = cropData,
                        ),
                    )
                }

                // 5. Clean up temp file
                tempFileStorage.deleteTemp(localFilePath)
            }.fold(
                onSuccess = {
                    _saveState.value = SaveState.Success
                    onSuccess()
                },
                onFailure = { e ->
                    Napier.e(e) { "Failed to save photo crop" }
                    _saveState.value = SaveState.Error
                },
            )
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
)

sealed interface SaveState {
    data object Idle : SaveState
    data object Uploading : SaveState
    data object Saving : SaveState
    data object Success : SaveState
    data object Error : SaveState
}
