package me.matsumo.travelog.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import me.matsumo.travelog.core.repository.SessionRepository

internal class LoginViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    val sessionStatus = sessionRepository.sessionStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SessionStatus.NotAuthenticated(false),
    )
}
