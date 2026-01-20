package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.Serializable

@Serializable
sealed interface GeoJsonProgressEvent {

    /**
     * 初期表示用の地域情報
     */
    @Serializable
    data class RegionInfo(
        val index: Int,
        val name: String,
        val level: Int,
        val thumbnailUrl: String?,
        val adm2Count: Int,
    )

    /**
     * 処理開始イベント（地域リスト付き）
     */
    @Serializable
    data class Started(
        val totalRegions: Int,
        val regions: List<RegionInfo> = emptyList(),
        val type: String = "started",
    ) : GeoJsonProgressEvent

    /**
     * 地域の処理開始イベント
     */
    @Serializable
    data class RegionStarted(
        val index: Int,
        val regionName: String,
        val level: Int,
        val adm2Count: Int,
        val type: String = "region_started",
    ) : GeoJsonProgressEvent

    /**
     * ADM2 進捗更新イベント
     */
    @Serializable
    data class Adm2Progress(
        val adm1Index: Int,
        val processedCount: Int,
        val totalCount: Int,
        val currentAdm2Name: String?,
        val type: String = "adm2_progress",
    ) : GeoJsonProgressEvent

    /**
     * 地域処理完了イベント（詳細情報付き）
     */
    @Serializable
    data class RegionCompleted(
        val index: Int,
        val regionName: String,
        val success: Boolean,
        val errorMessage: String? = null,
        val level: Int = 1,
        val nameEn: String? = null,
        val nameJa: String? = null,
        val isoCode: String? = null,
        val thumbnailUrl: String? = null,
        val wikipedia: String? = null,
        val centerLat: Double? = null,
        val centerLon: Double? = null,
        val adm2ProcessedCount: Int = 0,
        val adm2TotalCount: Int = 0,
        val processingTimeMs: Long = 0,
        val type: String = "region_completed",
    ) : GeoJsonProgressEvent

    /**
     * 全処理完了イベント
     */
    @Serializable
    data class Completed(
        val successCount: Int,
        val failCount: Int,
        val totalProcessingTimeMs: Long = 0,
        val adm2TotalCount: Int = 0,
        val type: String = "completed",
    ) : GeoJsonProgressEvent

    /**
     * エラーイベント
     */
    @Serializable
    data class Error(
        val message: String,
        val type: String = "error",
    ) : GeoJsonProgressEvent
}
