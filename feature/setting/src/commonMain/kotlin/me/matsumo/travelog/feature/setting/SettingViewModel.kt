package me.matsumo.travelog.feature.setting

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.Theme
import me.matsumo.travelog.core.repository.AppSettingRepository
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.SessionRepository
import me.matsumo.travelog.core.repository.UserRepository

class SettingViewModel(
    private val repository: AppSettingRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val geoAreaRepository: GeoAreaRepository,
) : ViewModel() {
    val setting = repository.setting
    val sessionStatus = sessionRepository.sessionStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionStatus.NotAuthenticated(false),
    )

    private val _cacheSize = MutableStateFlow<Long?>(null)
    val cacheSize: StateFlow<Long?> = _cacheSize.asStateFlow()

    init {
        loadCacheSize()
    }

    private fun loadCacheSize() {
        viewModelScope.launch {
            suspendRunCatching {
                geoAreaRepository.getCacheSize()
            }.onSuccess {
                _cacheSize.value = it
            }
        }
    }

    fun clearGeoAreaCache() {
        viewModelScope.launch {
            suspendRunCatching {
                geoAreaRepository.clearCache()
            }.onSuccess {
                _cacheSize.value = 0L
            }
        }
    }

    fun setTheme(theme: Theme) {
        viewModelScope.launch {
            repository.setTheme(theme)
        }
    }

    fun setUseDynamicColor(useDynamicColor: Boolean) {
        viewModelScope.launch {
            repository.setUseDynamicColor(useDynamicColor)
        }
    }

    fun setSeedColor(color: Color) {
        viewModelScope.launch {
            repository.setSeedColor(color)
        }
    }

    fun setDeveloperMode(developerMode: Boolean) {
        viewModelScope.launch {
            repository.setDeveloperMode(developerMode)
        }
    }

    fun logout() {
        viewModelScope.launch {
            suspendRunCatching {
                sessionRepository.signOut()
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val userId = sessionRepository.getCurrentUserInfo()?.id
            if (userId != null) {
                userRepository.deleteUser(userId)
                sessionRepository.signOut()
            }
        }
    }
}
