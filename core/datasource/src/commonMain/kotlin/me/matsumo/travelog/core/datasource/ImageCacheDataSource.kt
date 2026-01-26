package me.matsumo.travelog.core.datasource

import me.matsumo.travelog.core.model.db.Image
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

interface ImageCacheDataSource {
    suspend fun save(image: Image)
    suspend fun saveAll(images: List<Image>)
    suspend fun load(imageId: String, maxAge: Duration = 1.hours): Image?
    suspend fun loadAll(imageIds: List<String>, maxAge: Duration = 1.hours): List<Image>
    suspend fun clearAll()
}
