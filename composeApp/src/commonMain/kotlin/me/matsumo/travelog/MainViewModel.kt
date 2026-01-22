package me.matsumo.travelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.repository.AppSettingRepository
import me.matsumo.travelog.core.repository.SessionRepository
import me.matsumo.travelog.core.ui.state.AppStartupState

class MainViewModel(
    private val settingRepository: AppSettingRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _startupState = MutableStateFlow<AppStartupState>(AppStartupState.Loading)
    val startupState: StateFlow<AppStartupState> = _startupState.asStateFlow()

    private val _isAdsSdkInitialized = MutableStateFlow(false)
    val isAdsSdkInitialized = _isAdsSdkInitialized.asStateFlow()

    init {
        viewModelScope.launch {
            val hasSession = sessionRepository.hasValidSession()
            val setting = settingRepository.setting.first()

            _startupState.value = AppStartupState.Ready(
                setting = setting,
                isAuthenticated = hasSession,
            )
        }

        viewModelScope.launch {
            settingRepository.setting.collect { setting ->
                val currentState = _startupState.value
                if (currentState is AppStartupState.Ready) {
                    _startupState.value = currentState.copy(setting = setting)
                }
            }
        }
    }

    fun setAdsSdkInitialized() {
        _isAdsSdkInitialized.value = true
    }
}
