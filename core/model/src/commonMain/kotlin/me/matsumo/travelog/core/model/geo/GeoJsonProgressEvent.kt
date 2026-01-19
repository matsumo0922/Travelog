package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface GeoJsonProgressEvent {

    @Serializable
    @SerialName("started")
    data class Started(
        val totalRegions: Int,
    ) : GeoJsonProgressEvent

    @Serializable
    @SerialName("region_completed")
    data class RegionCompleted(
        val index: Int,
        val regionName: String,
        val success: Boolean,
        val errorMessage: String? = null,
    ) : GeoJsonProgressEvent

    @Serializable
    @SerialName("completed")
    data class Completed(
        val successCount: Int,
        val failCount: Int,
    ) : GeoJsonProgressEvent

    @Serializable
    @SerialName("error")
    data class Error(
        val message: String,
    ) : GeoJsonProgressEvent
}
