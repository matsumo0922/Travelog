package me.matsumo.travelog.core.usecase

import androidx.compose.runtime.Stable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import me.matsumo.travelog.core.repository.ImageRepository

class DeleteMapAreaImagesUseCase(
    private val imageRepository: ImageRepository,
) {
    operator fun invoke(
        imageIds: List<String>,
    ): Flow<DeleteProgress> = channelFlow {
        if (imageIds.isEmpty()) {
            send(DeleteProgress.Completed(0))
            return@channelFlow
        }

        val totalCount = imageIds.size
        send(DeleteProgress.Deleting(totalCount, 0))

        val semaphore = Semaphore(MAX_CONCURRENT_DELETES)
        val progressChannel = Channel<Unit>(Channel.UNLIMITED)
        var completedCount = 0

        launch {
            progressChannel.consumeEach {
                completedCount++
                send(DeleteProgress.Deleting(totalCount, completedCount))
            }
        }

        val results = imageIds.map { imageId ->
            async {
                semaphore.withPermit {
                    val result = deleteSingleImage(imageId)
                    progressChannel.send(Unit)
                    result
                }
            }
        }.awaitAll()

        progressChannel.close()

        val successCount = results.count { it }
        send(DeleteProgress.Completed(successCount))
    }

    private suspend fun deleteSingleImage(imageId: String): Boolean {
        return runCatching {
            imageRepository.deleteImage(imageId)
            true
        }.getOrDefault(false)
    }

    companion object {
        private const val MAX_CONCURRENT_DELETES = 3
    }
}

@Stable
sealed interface DeleteProgress {
    data class Deleting(
        val totalCount: Int,
        val completedCount: Int,
    ) : DeleteProgress

    data class Completed(
        val successCount: Int,
    ) : DeleteProgress
}
