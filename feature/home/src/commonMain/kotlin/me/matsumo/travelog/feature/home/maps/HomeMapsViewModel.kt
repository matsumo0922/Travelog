package me.matsumo.travelog.feature.home.maps

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.db.Map
import me.matsumo.travelog.core.repository.MapRepository
import me.matsumo.travelog.core.repository.SessionRepository
import me.matsumo.travelog.core.resource.Res
import me.matsumo.travelog.core.resource.error_network
import me.matsumo.travelog.core.ui.screen.ScreenState

class HomeMapsViewModel(
    private val sessionRepository: SessionRepository,
    private val mapRepository: MapRepository,
) : ViewModel() {
    val screenState: StateFlow<ScreenState<HomeMapsUiState>>
        field: MutableStateFlow<ScreenState<HomeMapsUiState>> = MutableStateFlow(ScreenState.Loading())

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            screenState.value = suspendRunCatching {
                val userInfo = sessionRepository.getCurrentUserInfo()!!
                val maps = mapRepository.getMapsByUserId(userInfo.id)

                HomeMapsUiState(maps)
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }
}

@Stable
data class HomeMapsUiState(
    val maps: List<Map>,
)
