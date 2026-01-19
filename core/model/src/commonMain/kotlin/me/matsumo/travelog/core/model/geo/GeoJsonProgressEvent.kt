package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.Serializable

@Serializable
sealed interface GeoJsonProgressEvent {

    @Serializable
    data class Started(
        val totalRegions: Int,
        val type: String = "started",
    ) : GeoJsonProgressEvent

    @Serializable
    data class RegionCompleted(
        val index: Int,
        val regionName: String,
        val success: Boolean,
        val errorMessage: String? = null,
        val type: String = "region_completed",
    ) : GeoJsonProgressEvent

    @Serializable
    data class Completed(
        val successCount: Int,
        val failCount: Int,
        val type: String = "completed",
    ) : GeoJsonProgressEvent

    @Serializable
    data class Error(
        val message: String,
        val type: String = "error",
    ) : GeoJsonProgressEvent
}
