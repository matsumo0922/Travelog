package me.matsumo.travelog.core.ui.state

import me.matsumo.travelog.core.model.AppSetting

sealed interface AppStartupState {
    data object Loading : AppStartupState

    data class Ready(
        val setting: AppSetting,
        val isAuthenticated: Boolean,
    ) : AppStartupState
}
