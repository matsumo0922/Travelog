package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import me.matsumo.travelog.core.model.db.Image

class ImageApi internal constructor(
    private val supabaseClient: SupabaseClient,
) {
    suspend fun createImage(image: Image): Image {
        return supabaseClient.from(TABLE_NAME)
            .insert(image) {
                select()
            }
            .decodeSingle()
    }

    suspend fun getImage(id: String): Image? {
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter { Image::id eq id }
            }
            .decodeSingleOrNull()
    }

    suspend fun getImagesByIds(ids: List<String>): List<Image> {
        if (ids.isEmpty()) return emptyList()
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter { Image::id isIn ids }
            }
            .decodeList()
    }

    suspend fun getImagesByMapRegionId(mapRegionId: String): List<Image> {
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter { Image::mapRegionId eq mapRegionId }
            }
            .decodeList()
    }

    suspend fun getImagesByUploaderUserId(userId: String): List<Image> {
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter { Image::uploaderUserId eq userId }
            }
            .decodeList()
    }

    suspend fun deleteImage(id: String) {
        supabaseClient.from(TABLE_NAME)
            .delete {
                filter { Image::id eq id }
            }
    }

    /**
     * Get preview images for a map region, sorted by taken_at descending.
     * Falls back to created_at if taken_at is null.
     */
    suspend fun getPreviewImagesByMapRegionId(
        mapRegionId: String,
        limit: Int,
    ): List<Image> {
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter { Image::mapRegionId eq mapRegionId }
                order(column = "taken_at", order = Order.DESCENDING, nullsFirst = false)
                limit(count = limit.toLong())
            }
            .decodeList()
    }

    /**
     * Get the count of images for a map region.
     */
    suspend fun getImageCountByMapRegionId(mapRegionId: String): Long {
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter { Image::mapRegionId eq mapRegionId }
            }
            .decodeList<Image>()
            .size
            .toLong()
    }

    companion object {
        private const val TABLE_NAME = "images"
    }
}
