package me.matsumo.travelog.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import me.matsumo.travelog.core.repository.GeoBoundaryRepository
import me.matsumo.travelog.core.repository.SessionRepository

internal class HomeViewModel(
    private val geoBoundaryRepository: GeoBoundaryRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    val sessionStatus = sessionRepository.sessionStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionStatus.Initializing,
    )
}