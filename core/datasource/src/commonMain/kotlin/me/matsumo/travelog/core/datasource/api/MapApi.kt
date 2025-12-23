package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import me.matsumo.travelog.core.model.db.Map

class MapApi internal constructor(
    private val supabaseClient: SupabaseClient,
) {
    suspend fun createMap(map: Map) {
        supabaseClient.from(TABLE_NAME)
            .insert(map)
    }

    suspend fun updateMap(map: Map) {
        supabaseClient.from(TABLE_NAME)
            .update(map)
    }

    suspend fun getMap(id: String): Map? {
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter { Map::id eq id }
            }
            .decodeSingleOrNull()
    }

    suspend fun getMapsByUserId(userId: String): List<Map> {
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter { Map::ownerUserId eq userId }
            }
            .decodeList()
    }

    suspend fun deleteMap(id: String) {
        supabaseClient.from(TABLE_NAME)
            .delete {
                filter { Map::id eq id }
            }
    }

    companion object {
        private const val TABLE_NAME = "maps"
    }
}
