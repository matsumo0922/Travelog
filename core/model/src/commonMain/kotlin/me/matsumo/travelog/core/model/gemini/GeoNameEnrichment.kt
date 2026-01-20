package me.matsumo.travelog.core.model.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Gemini API から返される個別エリアの名称補完結果
 */
@Serializable
data class GeoNameEnrichmentResult(
    @SerialName("name_en")
    val nameEn: String,
    @SerialName("name_ja")
    val nameJa: String,
    val confidence: Double,
    val reasoning: String? = null,
)

/**
 * Gemini API から返されるバッチ補完結果
 */
@Serializable
data class GeoNameBatchResult(
    val results: List<GeoNameEnrichmentItem>,
)

/**
 * バッチ処理における個別アイテムの結果
 */
@Serializable
data class GeoNameEnrichmentItem(
    @SerialName("adm_id")
    val admId: String,
    @SerialName("name_en")
    val nameEn: String,
    @SerialName("name_ja")
    val nameJa: String,
    val confidence: Double,
    val reasoning: String? = null,
)

/**
 * 処理対象のエリア情報
 */
@Serializable
data class MissingNameArea(
    val id: String,
    val admId: String,
    val countryCode: String,
    val level: Int,
    val name: String,
    val nameEn: String?,
    val nameJa: String?,
    val parentName: String?,
)

/**
 * 補完処理の適用ステータス
 */
enum class EnrichmentStatus {
    APPLIED,       // 自動適用（confidence >= 0.8）
    VALIDATED,     // パターン検証後に適用（0.5 <= confidence < 0.8）
    SKIPPED,       // スキップ（confidence < 0.5）
    ERROR,         // エラー発生
}

/**
 * SSE イベント用の Sealed Interface
 */
@Serializable
sealed interface GeoNameEnrichmentEvent {

    /**
     * 処理開始イベント
     */
    @Serializable
    data class Started(
        val totalCount: Int,
        val countryCode: String,
        val level: Int? = null,
        val type: String = "started",
    ) : GeoNameEnrichmentEvent

    /**
     * バッチ処理完了イベント
     */
    @Serializable
    data class BatchProcessed(
        val batchIndex: Int,
        val totalBatches: Int,
        val processedCount: Int,
        val appliedCount: Int,
        val validatedCount: Int,
        val skippedCount: Int,
        val type: String = "batch_processed",
    ) : GeoNameEnrichmentEvent

    /**
     * 個別アイテムの処理結果イベント
     */
    @Serializable
    data class ItemResult(
        val areaId: String,
        val admId: String,
        val originalName: String,
        val nameEn: String?,
        val nameJa: String?,
        val confidence: Double,
        val status: String,
        val reasoning: String? = null,
        val type: String = "item_result",
    ) : GeoNameEnrichmentEvent

    /**
     * 全処理完了イベント
     */
    @Serializable
    data class Completed(
        val totalProcessed: Int,
        val successCount: Int,
        val appliedCount: Int,
        val validatedCount: Int,
        val skippedCount: Int,
        val failedCount: Int,
        val elapsedMs: Long,
        val type: String = "completed",
    ) : GeoNameEnrichmentEvent

    /**
     * エラーイベント
     */
    @Serializable
    data class Error(
        val message: String,
        val type: String = "error",
    ) : GeoNameEnrichmentEvent
}
