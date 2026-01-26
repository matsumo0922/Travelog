package me.matsumo.travelog.core.usecase

import io.github.vinceglb.filekit.PlatformFile
import me.matsumo.travelog.core.repository.SessionRepository

class CreateMapWithIconUseCase(
    private val sessionRepository: SessionRepository,
    private val uploadMapIconUseCase: UploadMapIconUseCase,
    private val createMapUseCase: CreateMapUseCase,
) {
    /**
     * アイコン付きでマップを作成する
     *
     * @param rootGeoAreaId ルートGeoAreaのID
     * @param title マップタイトル
     * @param description マップ説明（オプション）
     * @param iconFile アイコン画像ファイル（オプション）
     * @return 作成結果
     */
    suspend operator fun invoke(
        rootGeoAreaId: String,
        title: String,
        description: String?,
        iconFile: PlatformFile?,
    ): Result {
        // 1. Validation
        if (title.isBlank()) {
            return Result.TitleRequired
        }

        // 2. Get current user ID
        val userId = sessionRepository.getCurrentUserInfo()?.id
            ?: return Result.UserNotLoggedIn

        // 3. Upload icon image if provided
        var iconImageId: String? = null
        if (iconFile != null) {
            val uploadResult = try {
                uploadMapIconUseCase(iconFile, userId)
            } catch (e: Exception) {
                return Result.UploadFailed(e)
            }
            iconImageId = uploadResult.imageId
        }

        // 4. Create map
        return try {
            createMapUseCase(
                userId = userId,
                rootGeoAreaId = rootGeoAreaId,
                title = title,
                description = description,
                iconImageId = iconImageId,
            )
            Result.Success
        } catch (e: Exception) {
            Result.CreateFailed(e)
        }
    }

    sealed interface Result {
        data object Success : Result
        data object TitleRequired : Result
        data object UserNotLoggedIn : Result
        data class UploadFailed(val cause: Throwable) : Result
        data class CreateFailed(val cause: Throwable) : Result
    }
}
