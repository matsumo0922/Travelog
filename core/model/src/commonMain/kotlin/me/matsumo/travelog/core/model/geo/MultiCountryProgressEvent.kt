package me.matsumo.travelog.core.model.geo

import kotlinx.serialization.Serializable

/**
 * 複数国処理（"All Countries"）用の進捗イベント
 */
@Serializable
sealed interface MultiCountryProgressEvent {

    /**
     * 国の基本情報
     */
    @Serializable
    data class CountryInfo(
        val index: Int,
        val code: String,
        val name: String,
        val flagUrl: String,
        val regionCount: Int,
    )

    /**
     * 全国処理開始イベント
     */
    @Serializable
    data class AllStarted(
        val countries: List<CountryInfo>,
        val totalCountries: Int,
        val type: String = "all_started",
    ) : MultiCountryProgressEvent

    /**
     * 1国の処理開始イベント
     */
    @Serializable
    data class CountryStarted(
        val countryIndex: Int,
        val countryCode: String,
        val countryName: String,
        val type: String = "country_started",
    ) : MultiCountryProgressEvent

    /**
     * 1国内の進捗イベント（内部イベントをラップ）
     */
    @Serializable
    data class CountryProgress(
        val countryIndex: Int,
        val countryCode: String,
        val innerEventJson: String,
        val innerEventType: String,
        val type: String = "country_progress",
    ) : MultiCountryProgressEvent

    /**
     * 1国の処理完了イベント
     */
    @Serializable
    data class CountryCompleted(
        val countryIndex: Int,
        val countryCode: String,
        val countryName: String,
        val success: Boolean,
        val successCount: Int = 0,
        val failCount: Int = 0,
        val totalRegions: Int = 0,
        val processingTimeMs: Long = 0,
        val errorMessage: String? = null,
        val type: String = "country_completed",
    ) : MultiCountryProgressEvent

    /**
     * 全国処理完了イベント
     */
    @Serializable
    data class AllCompleted(
        val totalCountries: Int,
        val successCount: Int,
        val failCount: Int,
        val totalTimeMs: Long,
        val type: String = "all_completed",
    ) : MultiCountryProgressEvent

    /**
     * エラーイベント
     */
    @Serializable
    data class Error(
        val message: String,
        val countryCode: String? = null,
        val type: String = "error",
    ) : MultiCountryProgressEvent
}
