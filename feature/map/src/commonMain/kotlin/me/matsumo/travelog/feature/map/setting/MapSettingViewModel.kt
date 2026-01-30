package me.matsumo.travelog.feature.map.setting

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
import me.matsumo.travelog.core.usecase.UpdateMapIconUseCase

class MapSettingViewModel(
    private val mapId: String,
    private val initialMap: Map?,
    private val initialTotalChildCount: Int?,
    private val initialRegions: List<MapRegion>?,
    private val mapRepository: MapRepository,
    private val mapRegionRepository: MapRegionRepository,
    private val geoAreaRepository: GeoAreaRepository,
    private val updateMapIconUseCase: UpdateMapIconUseCase,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapSettingUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<MapSettingUiState>> = _screenState.asStateFlow()

    private val _dialogState = MutableStateFlow<MapSettingDialogState>(MapSettingDialogState.None)
    val dialogState: StateFlow<MapSettingDialogState> = _dialogState.asStateFlow()

    private val _navigateToHome = MutableSharedFlow<Unit>()
    val navigateToHome: SharedFlow<Unit> = _navigateToHome.asSharedFlow()

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                // 初期データがあればそれを優先使用、なければAPIから取得
                val map = initialMap ?: mapRepository.getMap(mapId) ?: error("Map not found")
                val regions = initialRegions?.toImmutableList()
                    ?: mapRegionRepository.getMapRegionsByMapId(mapId).toImmutableList()

                // GeoArea は基本情報のみ取得（children は不要）
                val geoArea = geoAreaRepository.getAreaById(map.rootGeoAreaId) ?: error("Geo area not found")

                // totalChildCount は初期データがあればそれを使用、なければ children を取得して計算
                val totalChildCount = initialTotalChildCount
                    ?: geoAreaRepository.getChildren(map.rootGeoAreaId).size

                MapSettingUiState(
                    map = map,
                    geoArea = geoArea,
                    regions = regions,
                    totalChildCount = totalChildCount,
                    iconFile = null,
                )
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }

    fun showTitleEditDialog() {
        val currentState = (_screenState.value as? ScreenState.Idle)?.data ?: return
        _dialogState.value = MapSettingDialogState.TextEdit.Title(currentState.map.title)
    }

    fun showDescriptionEditDialog() {
        val currentState = (_screenState.value as? ScreenState.Idle)?.data ?: return
        _dialogState.value = MapSettingDialogState.TextEdit.Description(currentState.map.description.orEmpty())
    }

    fun showDeleteConfirmationDialog() {
        _dialogState.value = MapSettingDialogState.DeleteConfirmation
    }

    fun dismissDialog() {
        _dialogState.value = MapSettingDialogState.None
    }

    fun updateTitle(newTitle: String) {
        val currentState = (_screenState.value as? ScreenState.Idle)?.data ?: return
        if (newTitle.isBlank()) return

        viewModelScope.launch {
            _dialogState.value = MapSettingDialogState.Loading.UpdatingMap

            val updatedMap = currentState.map.copy(title = newTitle)
            val result = suspendRunCatching {
                mapRepository.updateMap(updatedMap)
            }

            if (result.isSuccess) {
                _screenState.update { ScreenState.Idle(currentState.copy(map = updatedMap)) }
                _dialogState.value = MapSettingDialogState.None
            } else {
                _dialogState.value = MapSettingDialogState.Error.UpdateFailed
            }
        }
    }

    fun updateDescription(newDescription: String) {
        val currentState = (_screenState.value as? ScreenState.Idle)?.data ?: return

        viewModelScope.launch {
            _dialogState.value = MapSettingDialogState.Loading.UpdatingMap

            val updatedMap = currentState.map.copy(description = newDescription.ifBlank { null })
            val result = suspendRunCatching {
                mapRepository.updateMap(updatedMap)
            }

            if (result.isSuccess) {
                _screenState.update { ScreenState.Idle(currentState.copy(map = updatedMap)) }
                _dialogState.value = MapSettingDialogState.None
            } else {
                _dialogState.value = MapSettingDialogState.Error.UpdateFailed
            }
        }
    }

    fun updateIconFile(file: PlatformFile?) {
        val currentState = (_screenState.value as? ScreenState.Idle)?.data ?: return
        _screenState.update { ScreenState.Idle(currentState.copy(iconFile = file)) }

        if (file != null) {
            uploadIcon(file)
        }
    }

    private fun uploadIcon(file: PlatformFile) {
        viewModelScope.launch {
            _dialogState.value = MapSettingDialogState.Loading.UploadingImage

            val latestState = (_screenState.value as? ScreenState.Idle)?.data ?: return@launch

            _dialogState.value = MapSettingDialogState.Loading.UpdatingMap

            val result = updateMapIconUseCase(
                map = latestState.map,
                iconFile = file,
            )

            when (result) {
                is UpdateMapIconUseCase.Result.Success -> {
                    _screenState.update {
                        val current = (it as? ScreenState.Idle)?.data ?: return@update it
                        ScreenState.Idle(current.copy(map = result.updatedMap, iconFile = null))
                    }
                    _dialogState.value = MapSettingDialogState.None
                }

                is UpdateMapIconUseCase.Result.UserNotLoggedIn,
                is UpdateMapIconUseCase.Result.UploadFailed,
                -> {
                    _dialogState.value = MapSettingDialogState.Error.UploadFailed
                }

                is UpdateMapIconUseCase.Result.UpdateFailed -> {
                    _dialogState.value = MapSettingDialogState.Error.UpdateFailed
                }
            }
        }
    }

    fun deleteMap() {
        viewModelScope.launch {
            _dialogState.value = MapSettingDialogState.Loading.DeletingMap

            val result = suspendRunCatching {
                mapRepository.deleteMap(mapId)
            }

            if (result.isSuccess) {
                _dialogState.value = MapSettingDialogState.None
                _navigateToHome.emit(Unit)
            } else {
                _dialogState.value = MapSettingDialogState.Error.DeleteFailed
            }
        }
    }
}

@Stable
data class MapSettingUiState(
    val map: Map,
    val geoArea: GeoArea,
    val regions: ImmutableList<MapRegion>,
    val totalChildCount: Int,
    val iconFile: PlatformFile?,
) {
    val regionCount: Int get() = regions.size
}

sealed interface MapSettingDialogState {
    data object None : MapSettingDialogState

    sealed interface TextEdit : MapSettingDialogState {
        val currentValue: String

        data class Title(override val currentValue: String) : TextEdit
        data class Description(override val currentValue: String) : TextEdit
    }

    data object DeleteConfirmation : MapSettingDialogState

    sealed interface Loading : MapSettingDialogState {
        data object UploadingImage : Loading
        data object UpdatingMap : Loading
        data object DeletingMap : Loading
    }

    sealed interface Error : MapSettingDialogState {
        data object UpdateFailed : Error
        data object UploadFailed : Error
        data object DeleteFailed : Error
    }
}
