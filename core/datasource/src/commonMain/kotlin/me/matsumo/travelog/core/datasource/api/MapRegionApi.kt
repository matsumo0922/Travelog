package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import me.matsumo.travelog.core.datasource.SessionStatusProvider
import me.matsumo.travelog.core.model.db.MapRegion

class MapRegionApi internal constructor(
    supabaseClient: SupabaseClient,
    sessionStatusProvider: SessionStatusProvider,
) : SupabaseApi(supabaseClient, sessionStatusProvider) {

    suspend fun createMapRegion(mapRegion: MapRegion): MapRegion = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .insert(mapRegion) { select() }
            .decodeSingle()
    }

    suspend fun updateMapRegion(mapRegion: MapRegion): MapRegion = withValidSession {
        val id = mapRegion.id
            ?: throw IllegalArgumentException("Cannot update MapRegion without id")

        supabaseClient.from(TABLE_NAME)
            .update(mapRegion) {
                filter { MapRegion::id eq id }
                select()
            }
            .decodeSingle()
    }

    suspend fun getMapRegion(id: String): MapRegion? = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .select {
                filter { MapRegion::id eq id }
            }
            .decodeSingleOrNull()
    }

    suspend fun getMapRegionsByMapId(mapId: String): List<MapRegion> = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .select {
                filter { MapRegion::mapId eq mapId }
            }
            .decodeList()
    }

    suspend fun getMapRegionsByMapIdAndGeoAreaId(
        mapId: String,
        geoAreaId: String,
    ): List<MapRegion> = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .select {
                filter {
                    MapRegion::mapId eq mapId
                    MapRegion::geoAreaId eq geoAreaId
                }
            }
            .decodeList()
    }

    suspend fun deleteMapRegion(id: String) = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .delete {
                filter { MapRegion::id eq id }
            }
    }

    companion object {
        private const val TABLE_NAME = "map_regions"
    }
}
