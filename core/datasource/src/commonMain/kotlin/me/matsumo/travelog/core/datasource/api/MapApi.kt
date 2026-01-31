package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import me.matsumo.travelog.core.datasource.SessionStatusProvider
import me.matsumo.travelog.core.model.db.Map

class MapApi internal constructor(
    supabaseClient: SupabaseClient,
    sessionStatusProvider: SessionStatusProvider,
) : SupabaseApi(supabaseClient, sessionStatusProvider) {

    suspend fun createMap(map: Map) = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .insert(map)
    }

    suspend fun updateMap(map: Map) = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .update(map) {
                filter { Map::id eq map.id }
            }
    }

    suspend fun getMap(id: String): Map? = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .select {
                filter { Map::id eq id }
            }
            .decodeSingleOrNull()
    }

    suspend fun getMapsByUserId(userId: String): List<Map> = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .select {
                filter { Map::ownerUserId eq userId }
            }
            .decodeList()
    }

    suspend fun deleteMap(id: String) = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .delete {
                filter { Map::id eq id }
            }
    }

    companion object {
        private const val TABLE_NAME = "maps"
    }
}
