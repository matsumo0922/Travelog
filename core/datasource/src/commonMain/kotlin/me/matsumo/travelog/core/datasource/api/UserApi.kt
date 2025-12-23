package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import me.matsumo.travelog.core.model.db.User

class UserApi(
    private val supabaseClient: SupabaseClient,
) {
    suspend fun createUser(displayName: String) {
        val user = User(
            displayName = displayName,
            handle = "",
            iconImageId = null,
        )

        supabaseClient.from(TABLE_NAME)
            .insert(user)
    }

    suspend fun updateUser(user: User) {
        supabaseClient.from(TABLE_NAME)
            .update(user)
    }

    suspend fun getUser(id: String): User? {
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter { User::id eq id }
            }
            .decodeSingleOrNull()
    }

    companion object {
        private const val TABLE_NAME = "users"
    }
}