package me.matsumo.travelog.feature.home.create.metadata

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.model.geo.GeoAreaLevel
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.SessionRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState
import me.matsumo.travelog.core.usecase.CreateMapUseCase
import me.matsumo.travelog.core.usecase.UploadMapIconUseCase

class MapCreateViewModel(
    private val selectedRegion: SupportedRegion,
    private val selectedAreaAdmId: String?,
    private val geoAreaRepository: GeoAreaRepository,
    private val sessionRepository: SessionRepository,
    private val uploadMapIconUseCase: UploadMapIconUseCase,
    private val createMapUseCase: CreateMapUseCase,
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<MapCreateUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<MapCreateUiState>> = _screenState

    private val _dialogState = MutableStateFlow<MapCreateDialogState>(MapCreateDialogState.None)
    val dialogState: StateFlow<MapCreateDialogState> = _dialogState.asStateFlow()

    private val _navigateToHome = MutableSharedFlow<Unit>()
    val navigateToHome: SharedFlow<Unit> = _navigateToHome.asSharedFlow()

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = suspendRunCatching {
                val selectedArea = if (selectedAreaAdmId != null) {
                    geoAreaRepository.getAreaByAdmId(selectedAreaAdmId)
                } else {
                    geoAreaRepository.getAreasByLevel(selectedRegion.code2, GeoAreaLevel.ADM0).firstOrNull()
                }

                val children = selectedArea?.id?.let { geoAreaRepository.getChildren(it) } ?: emptyList()
                val selectedAreaWithChildren = selectedArea!!.copy(children = children)

                MapCreateUiState(
                    region = selectedRegion,
                    selectedArea = selectedAreaWithChildren,
                    title = "",
                    description = "",
                    iconFile = null,
                )
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }

    fun updateTitle(title: String) {
        val currentState = (_screenState.value as? ScreenState.Idle)?.data ?: return
        _screenState.update {
            ScreenState.Idle(currentState.copy(title = title))
        }
    }

    fun updateDescription(description: String) {
        val currentState = (_screenState.value as? ScreenState.Idle)?.data ?: return
        _screenState.update {
            ScreenState.Idle(currentState.copy(description = description))
        }
    }

    fun updateIconFile(file: PlatformFile?) {
        val currentState = (_screenState.value as? ScreenState.Idle)?.data ?: return
        _screenState.update {
            ScreenState.Idle(currentState.copy(iconFile = file))
        }
    }

    fun dismissDialog() {
        _dialogState.value = MapCreateDialogState.None
    }

    fun createMap() {
        val currentState = (_screenState.value as? ScreenState.Idle)?.data ?: return
        val userId = sessionRepository.getCurrentUserInfo()?.id ?: return

        if (currentState.title.isBlank()) {
            _dialogState.value = MapCreateDialogState.Error.TitleRequired
            return
        }

        viewModelScope.launch {
            var iconImageId: String? = null

            // Upload icon image if provided
            if (currentState.iconFile != null) {
                _dialogState.value = MapCreateDialogState.Loading.UploadingImage

                val uploadResult = suspendRunCatching {
                    uploadMapIconUseCase(currentState.iconFile, userId)
                }

                if (uploadResult.isFailure) {
                    _dialogState.value = MapCreateDialogState.Error.UploadFailed
                    return@launch
                }

                iconImageId = uploadResult.getOrThrow().storageKey
            }

            // Create map
            _dialogState.value = MapCreateDialogState.Loading.CreatingMap

            val geoAreaId = currentState.selectedArea.id
            if (geoAreaId == null) {
                _dialogState.value = MapCreateDialogState.Error.CreateFailed
                return@launch
            }

            val mapResult = suspendRunCatching {
                createMapUseCase(
                    userId = userId,
                    rootGeoAreaId = geoAreaId,
                    title = currentState.title,
                    description = currentState.description,
                    iconImageId = iconImageId,
                )
            }

            if (mapResult.isFailure) {
                _dialogState.value = MapCreateDialogState.Error.CreateFailed
                return@launch
            }

            _dialogState.value = MapCreateDialogState.None
            _navigateToHome.emit(Unit)
        }
    }
}

@Stable
data class MapCreateUiState(
    val region: SupportedRegion,
    val selectedArea: GeoArea,
    val title: String,
    val description: String,
    val iconFile: PlatformFile?,
)

sealed interface MapCreateDialogState {
    data object None : MapCreateDialogState

    sealed interface Loading : MapCreateDialogState {
        data object UploadingImage : Loading
        data object CreatingMap : Loading
    }

    sealed interface Error : MapCreateDialogState {
        data object TitleRequired : Error
        data object UploadFailed : Error
        data object CreateFailed : Error
    }
}
