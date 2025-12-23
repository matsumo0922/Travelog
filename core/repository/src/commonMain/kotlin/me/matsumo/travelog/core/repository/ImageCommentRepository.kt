package me.matsumo.travelog.core.repository

import me.matsumo.travelog.core.datasource.api.ImageCommentApi
import me.matsumo.travelog.core.model.db.ImageComment

class ImageCommentRepository(
    private val imageCommentApi: ImageCommentApi,
) {
    suspend fun createImageComment(imageComment: ImageComment) {
        imageCommentApi.createImageComment(imageComment)
    }

    suspend fun updateImageComment(imageComment: ImageComment) {
        imageCommentApi.updateImageComment(imageComment)
    }

    suspend fun getImageComment(id: String): ImageComment? {
        return imageCommentApi.getImageComment(id)
    }

    suspend fun getImageCommentsByImageId(imageId: String): List<ImageComment> {
        return imageCommentApi.getImageCommentsByImageId(imageId)
    }

    suspend fun deleteImageComment(id: String) {
        imageCommentApi.deleteImageComment(id)
    }
}
