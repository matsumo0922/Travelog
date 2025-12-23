package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import me.matsumo.travelog.core.model.db.ImageComment

class ImageCommentApi internal constructor(
    private val supabaseClient: SupabaseClient,
) {
    suspend fun createImageComment(imageComment: ImageComment) {
        supabaseClient.from(TABLE_NAME)
            .insert(imageComment)
    }

    suspend fun updateImageComment(imageComment: ImageComment) {
        supabaseClient.from(TABLE_NAME)
            .update(imageComment)
    }

    suspend fun getImageComment(id: String): ImageComment? {
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter { ImageComment::id eq id }
            }
            .decodeSingleOrNull()
    }

    suspend fun getImageCommentsByImageId(imageId: String): List<ImageComment> {
        return supabaseClient.from(TABLE_NAME)
            .select {
                filter { ImageComment::imageId eq imageId }
            }
            .decodeList()
    }

    suspend fun deleteImageComment(id: String) {
        supabaseClient.from(TABLE_NAME)
            .delete {
                filter { ImageComment::id eq id }
            }
    }

    companion object {
        private const val TABLE_NAME = "image_comments"
    }
}
