package me.matsumo.travelog.core.datasource.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import me.matsumo.travelog.core.datasource.SessionStatusProvider
import kotlin.time.Duration

class StorageApi internal constructor(
    supabaseClient: SupabaseClient,
    sessionStatusProvider: SessionStatusProvider,
) : SupabaseApi(supabaseClient, sessionStatusProvider) {

    suspend fun uploadImage(
        bucketName: String,
        path: String,
        data: ByteArray,
        contentType: String,
    ): String = withValidSession {
        val bucket = supabaseClient.storage.from(bucketName)
        val ktorContentType = ContentType.parse(contentType)
        bucket.upload(path, data) {
            this.contentType = ktorContentType
            upsert = true
        }
        bucket.publicUrl(path)
    }

    suspend fun deleteImage(bucket: String, path: String) = withValidSession {
        supabaseClient.storage.from(bucket).delete(path)
    }

    fun getPublicUrl(bucket: String, path: String): String {
        return supabaseClient.storage.from(bucket).publicUrl(path)
    }

    suspend fun createSignedUrl(bucket: String, path: String, expiresIn: Duration): String = withValidSession {
        supabaseClient.storage.from(bucket).createSignedUrl(path, expiresIn)
    }

    companion object {
        const val BUCKET_MAP_ICONS = "map-icons"
        const val BUCKET_MAP_REGION_IMAGES = "map-region-images"
    }
}
