package me.matsumo.travelog.core.usecase

import io.github.vinceglb.filekit.PlatformFile
import me.matsumo.travelog.core.model.db.Map
import me.matsumo.travelog.core.repository.MapRepository
import me.matsumo.travelog.core.repository.SessionRepository

class UpdateMapIconUseCase(
    private val mapRepository: MapRepository,
    private val sessionRepository: SessionRepository,
    private val uploadMapIconUseCase: UploadMapIconUseCase,
) {
    /**
     * マップアイコンを更新する（アップロード + Map更新）
     *
     * @param map 更新対象のMap
     * @param iconFile アイコン画像ファイル
     * @return 更新されたMapを含む結果
     */
    suspend operator fun invoke(
        map: Map,
        iconFile: PlatformFile,
    ): Result {
        // 1. Get current user ID
        val userId = sessionRepository.getCurrentUserInfo()?.id
            ?: return Result.UserNotLoggedIn

        // 2. Upload icon image
        val uploadResult = try {
            uploadMapIconUseCase(iconFile, userId)
        } catch (e: Exception) {
            return Result.UploadFailed(e)
        }

        // 3. Update map with new icon info
        val updatedMap = map.copy(
            iconImageId = uploadResult.imageId,
            iconImageUrl = uploadResult.publicUrl,
        )

        return try {
            mapRepository.updateMap(updatedMap)
            Result.Success(updatedMap)
        } catch (e: Exception) {
            Result.UpdateFailed(e)
        }
    }

    sealed interface Result {
        data class Success(val updatedMap: Map) : Result
        data object UserNotLoggedIn : Result
        data class UploadFailed(val cause: Throwable) : Result
        data class UpdateFailed(val cause: Throwable) : Result
    }
}
