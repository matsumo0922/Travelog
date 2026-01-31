package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.first
import me.matsumo.travelog.core.common.exception.SessionNotAuthenticatedException
import me.matsumo.travelog.core.datasource.SessionStatusProvider

abstract class SupabaseApi(
    protected val supabaseClient: SupabaseClient,
    private val sessionStatusProvider: SessionStatusProvider,
) {
    protected suspend fun <T> withValidSession(block: suspend () -> T): T {
        val status = sessionStatusProvider.sessionStatus.first {
            it !is SessionStatus.Initializing
        }

        if (status !is SessionStatus.Authenticated) {
            throw SessionNotAuthenticatedException()
        }

        return block()
    }
}
