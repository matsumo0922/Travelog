package me.matsumo.travelog.core.usecase

import me.matsumo.travelog.core.model.db.CropData
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import me.matsumo.travelog.core.repository.MapRegionRepository
import me.matsumo.travelog.core.repository.SessionRepository

class SaveMapRegionPhotoUseCase(
    private val mapRegionRepository: MapRegionRepository,
    private val sessionRepository: SessionRepository,
    private val uploadMapRegionImageUseCase: UploadMapRegionImageUseCase,
    private val tempFileStorage: TempFileStorage,
) {
    /**
     * 写真を保存する（アップロード + MapRegion作成/更新）
     *
     * @param mapId マップID
     * @param geoAreaId 地域ID
     * @param localFilePath 一時ファイルパス
     * @param geoArea GeoArea（クロップ生成に使用）
     * @param cropData クロップデータ
     * @param existingRegion 既存のMapRegion（更新の場合）
     * @return 処理結果
     */
    suspend operator fun invoke(
        mapId: String,
        geoAreaId: String,
        localFilePath: String,
        geoArea: GeoArea,
        cropData: CropData,
        existingRegion: MapRegion?,
    ): Result {
        // 1. Load file from temp storage
        val file = tempFileStorage.loadFromTemp(localFilePath)
            ?: return Result.TempFileNotFound

        // 2. Get current user ID
        val userId = sessionRepository.getCurrentUserInfo()?.id
            ?: return Result.UserNotLoggedIn

        // 3. Upload original and cropped images
        val uploadResult = try {
            uploadMapRegionImageUseCase(
                file = file,
                geoArea = geoArea,
                cropData = cropData,
                userId = userId,
            )
        } catch (e: Exception) {
            return Result.UploadFailed(e)
        }

        // 4. Create or update MapRegion
        try {
            if (existingRegion != null) {
                mapRegionRepository.updateMapRegion(
                    existingRegion.copy(
                        representativeImageId = uploadResult.originalImageId,
                        representativeCroppedImageId = uploadResult.croppedImageId,
                        cropData = cropData,
                    ),
                )
            } else {
                mapRegionRepository.createMapRegion(
                    MapRegion(
                        mapId = mapId,
                        geoAreaId = geoAreaId,
                        representativeImageId = uploadResult.originalImageId,
                        representativeCroppedImageId = uploadResult.croppedImageId,
                        cropData = cropData,
                    ),
                )
            }
        } catch (e: Exception) {
            return Result.SaveFailed(e)
        }

        // 5. Clean up temp file
        tempFileStorage.deleteTemp(localFilePath)

        return Result.Success
    }

    sealed interface Result {
        data object Success : Result
        data object TempFileNotFound : Result
        data object UserNotLoggedIn : Result
        data class UploadFailed(val cause: Throwable) : Result
        data class SaveFailed(val cause: Throwable) : Result
    }
}
