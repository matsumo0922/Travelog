package me.matsumo.travelog.core.datasource

import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow

interface SessionStatusProvider {
    val sessionStatus: Flow<SessionStatus>
}
