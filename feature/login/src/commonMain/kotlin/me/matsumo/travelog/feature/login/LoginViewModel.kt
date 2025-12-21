package me.matsumo.travelog.feature.login

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class LoginViewModel : ViewModel() {
    val uiState: StateFlow<LoginUiState>
        field: MutableStateFlow<LoginUiState> = MutableStateFlow(LoginUiState)
}

@Stable
internal object LoginUiState
