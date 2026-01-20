package model

import kotlinx.serialization.Serializable

/**
 * バッチ処理全体の結果
 */
@Serializable
data class BatchResult(
    val totalCountries: Int,
    val successCount: Int,
    val failCount: Int,
    val totalTimeMs: Long,
    val countryResults: List<CountryResult>,
    val executedAt: String,
)

/**
 * 国別の処理結果
 */
@Serializable
data class CountryResult(
    val countryCode: String,
    val countryName: String,
    val success: Boolean,
    val processedRegions: Int = 0,
    val failedRegions: Int = 0,
    val processingTimeMs: Long = 0,
    val errorMessage: String? = null,
)

/**
 * GeoJSONバッチ処理リクエスト
 */
@Serializable
data class BatchGeoJsonRequest(
    val targetCountries: List<String>? = null, // null = 全国
)

/**
 * 名前補完バッチ処理リクエスト
 */
@Serializable
data class BatchGeoNamesRequest(
    val targetCountries: List<String>? = null, // null = 全国
    val batchSize: Int = 10,
    val dryRun: Boolean = false,
)

/**
 * 名前補完バッチ処理結果
 */
@Serializable
data class BatchGeoNamesResult(
    val totalCountries: Int,
    val successCount: Int,
    val failCount: Int,
    val totalTimeMs: Long,
    val totalApplied: Int,
    val totalValidated: Int,
    val totalSkipped: Int,
    val totalFailed: Int,
    val countryResults: List<GeoNamesCountryResult>,
    val executedAt: String,
    val dryRun: Boolean,
)

/**
 * 名前補完の国別結果
 */
@Serializable
data class GeoNamesCountryResult(
    val countryCode: String,
    val countryName: String,
    val success: Boolean,
    val appliedCount: Int = 0,
    val validatedCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val processingTimeMs: Long = 0,
    val errorMessage: String? = null,
)
