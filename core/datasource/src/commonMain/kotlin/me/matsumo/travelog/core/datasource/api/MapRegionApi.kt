package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import me.matsumo.travelog.core.model.db.MapRegion

class MapRegionApi internal constructor(
    private val supabaseClient: SupabaseClient,
) {
    suspend fun createMapRegion(mapRegion: MapRegion) {
        supabaseClient.from(TABLE_NAME)
            .insert(mapRegion)
    }

    suspend fun updateMapRegion(mapRegion: MapRegion) {
        val id = mapRegion.id
            ?: throw IllegalArgumentException("Cannot update MapRegion without id")

        supabaseClient.from(TABLE_NAME)
            .update(mapRegion) {
                filter { MapRegion::id eq id }
            }
    }

    suspend fun getMapRegion(id: String): MapRegion? {
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter { MapRegion::id eq id }
            }
            .decodeSingleOrNull()
    }

    suspend fun getMapRegionsByMapId(mapId: String): List<MapRegion> {
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter { MapRegion::mapId eq mapId }
            }
            .decodeList()
    }

    suspend fun getMapRegionsByMapIdAndGeoAreaId(
        mapId: String,
        geoAreaId: String,
    ): List<MapRegion> {
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter {
                    MapRegion::mapId eq mapId
                    MapRegion::geoAreaId eq geoAreaId
                }
            }
            .decodeList()
    }

    suspend fun deleteMapRegion(id: String) {
        supabaseClient.from(TABLE_NAME)
            .delete {
                filter { MapRegion::id eq id }
            }
    }

    companion object {
        private const val TABLE_NAME = "map_regions"
    }
}
