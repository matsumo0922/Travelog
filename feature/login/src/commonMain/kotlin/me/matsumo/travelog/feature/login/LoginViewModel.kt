package me.matsumo.travelog.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.matsumo.travelog.core.common.suspendRunCatching
import me.matsumo.travelog.core.model.db.User
import me.matsumo.travelog.core.repository.SessionRepository
import me.matsumo.travelog.core.repository.UserRepository

internal class LoginViewModel(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _authenticatedTrigger = Channel<Unit>(Channel.BUFFERED)
    val authenticatedTrigger = _authenticatedTrigger.receiveAsFlow()

    private val _authenticationFailedTrigger = Channel<Unit>(Channel.BUFFERED)
    val authenticationFailedTrigger = _authenticationFailedTrigger.receiveAsFlow()

    val sessionStatus = sessionRepository.sessionStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SessionStatus.NotAuthenticated(false),
    )

    init {
        viewModelScope.launch {
            sessionRepository.sessionStatus.collect {
                if (it is SessionStatus.Authenticated) {
                    suspendRunCatching {
                        val id = it.session.user!!.id
                        val currentUser = userRepository.getUser(id) ?: User(
                            id = id,
                            handle = id,
                            displayName = "Unknown",
                            iconImageId = null,
                        )

                        userRepository.upsertUser(currentUser)
                    }.onSuccess {
                        _authenticatedTrigger.send(Unit)
                    }.onFailure {
                        _authenticationFailedTrigger.send(Unit)
                    }
                }
            }
        }
    }

    fun signInWithGoogleOAuth() {
        viewModelScope.launch {
            sessionRepository.signInWithGoogleOAuth()
        }
    }

    fun signInWithAppleOAuth() {
        viewModelScope.launch {
            sessionRepository.signInWithAppleOAuth()
        }
    }
}
