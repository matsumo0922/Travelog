package me.matsumo.travelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.status.SessionStatus
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
            // ストレージからセッションをロード
            sessionRepository.hasValidSession()

            // sessionStatus が Initializing 以外になるまで待機
            val status = sessionRepository.sessionStatus.first {
                it !is SessionStatus.Initializing
            }

            val setting = settingRepository.setting.first()
            val isAuthenticated = status is SessionStatus.Authenticated

            _startupState.value = AppStartupState.Ready(
                setting = setting,
                isAuthenticated = isAuthenticated,
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
