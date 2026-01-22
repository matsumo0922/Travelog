package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType

class StorageApi internal constructor(
    private val supabaseClient: SupabaseClient,
) {
    suspend fun uploadImage(
        bucketName: String,
        path: String,
        data: ByteArray,
        contentType: String,
    ): String {
        val bucket = supabaseClient.storage.from(bucketName)
        val ktorContentType = ContentType.parse(contentType)
        bucket.upload(path, data) {
            this.contentType = ktorContentType
            upsert = true
        }
        return bucket.publicUrl(path)
    }

    suspend fun deleteImage(bucket: String, path: String) {
        supabaseClient.storage.from(bucket).delete(path)
    }

    fun getPublicUrl(bucket: String, path: String): String {
        return supabaseClient.storage.from(bucket).publicUrl(path)
    }

    companion object {
        const val BUCKET_MAP_ICONS = "map-icons"
    }
}
