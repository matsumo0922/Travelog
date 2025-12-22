package me.matsumo.travelog

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.Foundation.NSURL

object SupabaseAuthHandler : KoinComponent {
    private val supabaseClient: SupabaseClient by inject()

    fun handleDeeplink(url: NSURL) {
        try {
            Napier.d { "Deeplink: $url" }
            supabaseClient.handleDeeplinks(url)
        } catch (e: Exception) {
            Napier.w(e) { "Failed to handle deeplink." }
        }
    }
}