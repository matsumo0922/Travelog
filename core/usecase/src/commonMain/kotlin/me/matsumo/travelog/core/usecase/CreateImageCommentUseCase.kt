package me.matsumo.travelog.core.usecase

import me.matsumo.travelog.core.model.db.ImageComment
import me.matsumo.travelog.core.repository.ImageCommentRepository
import me.matsumo.travelog.core.repository.SessionRepository

class CreateImageCommentUseCase(
    private val sessionRepository: SessionRepository,
    private val imageCommentRepository: ImageCommentRepository,
) {
    suspend operator fun invoke(imageId: String, body: String): Result {
        if (body.isBlank()) return Result.InvalidBody

        val userId = sessionRepository.getCurrentUserInfo()?.id
            ?: return Result.UserNotLoggedIn

        val newComment = ImageComment(
            imageId = imageId,
            authorUserId = userId,
            body = body,
        )

        return try {
            imageCommentRepository.createImageComment(newComment)
            val latestComments = imageCommentRepository.getImageCommentsByImageId(imageId)
            Result.Success(latestComments)
        } catch (e: Exception) {
            Result.Failed(e)
        }
    }

    sealed interface Result {
        data class Success(val comments: List<ImageComment>) : Result
        data object InvalidBody : Result
        data object UserNotLoggedIn : Result
        data class Failed(val cause: Throwable) : Result
    }
}
