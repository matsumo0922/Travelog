package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import me.matsumo.travelog.core.datasource.SessionStatusProvider
import me.matsumo.travelog.core.model.db.User

class UserApi internal constructor(
    supabaseClient: SupabaseClient,
    sessionStatusProvider: SessionStatusProvider,
) : SupabaseApi(supabaseClient, sessionStatusProvider) {

    suspend fun upsertUser(user: User) = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .upsert(user)
    }

    suspend fun getUser(id: String): User? = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .select {
                filter { User::id eq id }
            }
            .decodeSingleOrNull()
    }

    suspend fun deleteUser(id: String) = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .delete {
                filter { User::id eq id }
            }
    }

    companion object {
        private const val TABLE_NAME = "users"
    }
}
