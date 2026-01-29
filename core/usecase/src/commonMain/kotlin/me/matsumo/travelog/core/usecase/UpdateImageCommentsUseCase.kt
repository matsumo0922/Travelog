package me.matsumo.travelog.core.usecase

import me.matsumo.travelog.core.model.db.ImageComment
import me.matsumo.travelog.core.repository.ImageCommentRepository

class UpdateImageCommentsUseCase(
    private val imageCommentRepository: ImageCommentRepository,
) {
    suspend operator fun invoke(comments: List<ImageComment>): Result {
        if (comments.isEmpty()) return Result.NothingToUpdate

        return try {
            comments.forEach { comment ->
                imageCommentRepository.updateImageComment(comment)
            }
            Result.Success
        } catch (e: Exception) {
            Result.Failed(e)
        }
    }

    sealed interface Result {
        data object Success : Result
        data object NothingToUpdate : Result
        data class Failed(val cause: Throwable) : Result
    }
}
