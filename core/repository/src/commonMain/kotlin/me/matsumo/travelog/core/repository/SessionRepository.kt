package me.matsumo.travelog.core.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow

class SessionRepository(
    private val supabaseClient: SupabaseClient,
) {
    val sessionStatus: Flow<SessionStatus> = supabaseClient.auth.sessionStatus

    suspend fun hasValidSession(): Boolean {
        return supabaseClient.auth.loadFromStorage()
    }

    suspend fun signInWithGoogleOAuth() {
        supabaseClient.auth.signInWith(Google)
    }

    suspend fun signInWithAppleOAuth() {
        supabaseClient.auth.signInWith(Apple)
    }

    suspend fun signOut() {
        supabaseClient.auth.signOut()
    }

    companion object {
        private const val REDIRECT_URL = "https://travelog.dev/auth/v1/callback"
    }
}