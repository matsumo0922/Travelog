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
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState
import me.matsumo.travelog.core.usecase.CreateMapWithIconUseCase

class MapCreateViewModel(
    private val selectedRegion: SupportedRegion,
    private val selectedAreaAdmId: String?,
    private val geoAreaRepository: GeoAreaRepository,
    private val createMapWithIconUseCase: CreateMapWithIconUseCase,
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

        val geoAreaId = currentState.selectedArea.id
        if (geoAreaId == null) {
            _dialogState.value = MapCreateDialogState.Error.CreateFailed
            return
        }

        viewModelScope.launch {
            _dialogState.value = if (currentState.iconFile != null) {
                MapCreateDialogState.Loading.UploadingImage
            } else {
                MapCreateDialogState.Loading.CreatingMap
            }

            val result = createMapWithIconUseCase(
                rootGeoAreaId = geoAreaId,
                title = currentState.title,
                description = currentState.description.ifBlank { null },
                iconFile = currentState.iconFile,
            )

            when (result) {
                is CreateMapWithIconUseCase.Result.Success -> {
                    _dialogState.value = MapCreateDialogState.None
                    _navigateToHome.emit(Unit)
                }

                is CreateMapWithIconUseCase.Result.TitleRequired -> {
                    _dialogState.value = MapCreateDialogState.Error.TitleRequired
                }

                is CreateMapWithIconUseCase.Result.UserNotLoggedIn,
                is CreateMapWithIconUseCase.Result.UploadFailed,
                -> {
                    _dialogState.value = MapCreateDialogState.Error.UploadFailed
                }

                is CreateMapWithIconUseCase.Result.CreateFailed -> {
                    _dialogState.value = MapCreateDialogState.Error.CreateFailed
                }
            }
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
