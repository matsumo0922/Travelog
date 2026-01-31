package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import me.matsumo.travelog.core.datasource.SessionStatusProvider
import me.matsumo.travelog.core.model.db.ImageComment

class ImageCommentApi internal constructor(
    supabaseClient: SupabaseClient,
    sessionStatusProvider: SessionStatusProvider,
) : SupabaseApi(supabaseClient, sessionStatusProvider) {

    suspend fun createImageComment(imageComment: ImageComment) = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .insert(imageComment)
    }

    suspend fun updateImageComment(imageComment: ImageComment) = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .update(imageComment)
    }

    suspend fun getImageComment(id: String): ImageComment? = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .select {
                filter { ImageComment::id eq id }
            }
            .decodeSingleOrNull()
    }

    suspend fun getImageCommentsByImageId(imageId: String): List<ImageComment> = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .select {
                filter { ImageComment::imageId eq imageId }
            }
            .decodeList()
    }

    suspend fun deleteImageComment(id: String) = withValidSession {
        supabaseClient.from(TABLE_NAME)
            .delete {
                filter { ImageComment::id eq id }
            }
    }

    companion object {
        private const val TABLE_NAME = "image_comments"
    }
}
