package me.matsumo.travelog.core.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow

class SessionRepository(
    private val supabaseClient: SupabaseClient,
) {
    val sessionStatus: Flow<SessionStatus> = supabaseClient.auth.sessionStatus

    fun getCurrentUserInfo(): UserInfo? {
        return supabaseClient.auth.currentUserOrNull()
    }

    suspend fun hasValidSession(): Boolean {
        return supabaseClient.auth.loadFromStorage()
    }

    suspend fun signInWithGoogleOAuth() {
        supabaseClient.auth.signInWith(
            provider = Google,
            redirectUrl = REDIRECT_URL,
        )
    }

    suspend fun signInWithAppleOAuth() {
        supabaseClient.auth.signInWith(
            provider = Apple,
            redirectUrl = REDIRECT_URL,
        )
    }

    suspend fun signOut() {
        supabaseClient.auth.signOut()
    }

    companion object {
        const val REDIRECT_URL = "https://travelog.dev/auth/v1/callback"
    }
}
