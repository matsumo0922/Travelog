package me.matsumo.travelog.core.usecase

import me.matsumo.travelog.core.model.db.Map
import me.matsumo.travelog.core.repository.MapRepository

class CreateMapUseCase(
    private val mapRepository: MapRepository,
) {
    suspend operator fun invoke(
        userId: String,
        rootGeoAreaId: String,
        title: String,
        description: String?,
        iconImageId: String?,
    ) {
        val map = Map(
            ownerUserId = userId,
            rootGeoAreaId = rootGeoAreaId,
            title = title.trim(),
            description = description?.takeIf { it.isNotBlank() }?.trim(),
            iconImageId = iconImageId,
        )
        mapRepository.createMap(map)
    }
}
