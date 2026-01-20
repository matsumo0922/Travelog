package service

import me.matsumo.travelog.core.model.SupportedRegion
import me.matsumo.travelog.core.model.gemini.GeoNameEnrichmentEvent
import me.matsumo.travelog.core.repository.Adm1ProcessingEvent
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.GeoBoundaryRepository
import me.matsumo.travelog.core.repository.GeoNameEnrichmentRepository
import model.BatchGeoJsonRequest
import model.BatchGeoNamesRequest
import model.BatchGeoNamesResult
import model.BatchResult
import model.CountryResult
import model.GeoNamesCountryResult
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class BatchProcessingService(
    private val geoBoundaryRepository: GeoBoundaryRepository,
    private val geoAreaRepository: GeoAreaRepository,
    private val geoNameEnrichmentRepository: GeoNameEnrichmentRepository,
) {
    private val logger = LoggerFactory.getLogger(BatchProcessingService::class.java)

    /**
     * GeoJSONバッチ処理を実行
     * 既存SSEルート (/geojson/all/stream) のロジックを同期的に実行
     */
    suspend fun processAllGeoJson(request: BatchGeoJsonRequest): BatchResult {
        val startTime = java.lang.System.currentTimeMillis()
        val countries = request.targetCountries
            ?.mapNotNull { code -> SupportedRegion.all.find { it.code2 == code } }
            ?: SupportedRegion.all

        logger.info("BatchProcessingService: Starting GeoJSON batch processing for ${countries.size} countries")

        val countryResults = mutableListOf<CountryResult>()
        var totalSuccess = 0
        var totalFail = 0

        countries.forEach { supportedRegion ->
            val countryStartTime = java.lang.System.currentTimeMillis()
            val countryCode = supportedRegion.code2

            logger.info("BatchProcessingService: Processing country ${supportedRegion.nameEn} ($countryCode)")

            try {
                val result = processCountryGeoJson(countryCode, supportedRegion)
                countryResults.add(result.copy(processingTimeMs = java.lang.System.currentTimeMillis() - countryStartTime))

                if (result.success) {
                    totalSuccess++
                    logger.info(
                        "BatchProcessingService: ${supportedRegion.nameEn} completed - " +
                                "${result.processedRegions} regions processed",
                    )
                } else {
                    totalFail++
                    logger.error(
                        "BatchProcessingService: ${supportedRegion.nameEn} failed - ${result.errorMessage}",
                    )
                }
            } catch (e: Exception) {
                totalFail++
                logger.error("BatchProcessingService: ${supportedRegion.nameEn} failed with exception", e)
                countryResults.add(
                    CountryResult(
                        countryCode = countryCode,
                        countryName = supportedRegion.nameEn,
                        success = false,
                        errorMessage = e.message,
                        processingTimeMs = java.lang.System.currentTimeMillis() - countryStartTime,
                    ),
                )
            }
        }

        val totalTime = java.lang.System.currentTimeMillis() - startTime
        logger.info(
            "BatchProcessingService: GeoJSON batch completed - " +
                    "$totalSuccess success, $totalFail fail, ${totalTime}ms",
        )

        return BatchResult(
            totalCountries = countries.size,
            successCount = totalSuccess,
            failCount = totalFail,
            totalTimeMs = totalTime,
            countryResults = countryResults,
            executedAt = currentIsoDateTime(),
        )
    }

    private suspend fun processCountryGeoJson(
        countryCode: String,
        supportedRegion: SupportedRegion,
    ): CountryResult {
        var successCount = 0
        var failCount = 0

        // Get country area (ADM0)
        val countryInfo = GeoBoundaryRepository.CountryInfo(
            name = supportedRegion.nameEn,
            nameEn = supportedRegion.nameEn,
            nameJa = null,
            wikipedia = null,
            thumbnailUrl = supportedRegion.flagUrl,
        )
        val countryArea = geoBoundaryRepository.getCountryArea(countryCode, countryInfo)

        // Get ADM1 regions
        val regions = geoBoundaryRepository.getEnrichedCountries(countryCode)

        // Upsert country (ADM0)
        val countryId = runCatching { geoAreaRepository.upsertArea(countryArea) }
            .onSuccess { successCount++ }
            .onFailure { failCount++ }
            .getOrNull()

        if (countryId == null) {
            return CountryResult(
                countryCode = countryCode,
                countryName = supportedRegion.nameEn,
                success = false,
                processedRegions = successCount,
                failedRegions = failCount,
                errorMessage = "Failed to upsert country area",
            )
        }

        // Process ADM1 regions using Flow (same as SSE route)
        geoBoundaryRepository.getEnrichedAllAdminsAsFlow(countryCode, regions).collect { event ->
            when (event) {
                is Adm1ProcessingEvent.Started -> {
                    logger.debug("BatchProcessingService: Processing region ${event.regionName}")
                }

                is Adm1ProcessingEvent.Completed -> {
                    event.result
                        .onSuccess { adm1GeoArea ->
                            val areaWithParent = adm1GeoArea.copy(parentId = countryId)
                            runCatching {
                                val adm1Id = geoAreaRepository.upsertArea(areaWithParent)
                                // Upsert ADM2 children
                                adm1GeoArea.children.forEach { adm2 ->
                                    runCatching { geoAreaRepository.upsertArea(adm2.copy(parentId = adm1Id)) }
                                }
                            }
                                .onSuccess { successCount++ }
                                .onFailure { failCount++ }
                        }
                        .onFailure { failCount++ }
                }
            }
        }

        return CountryResult(
            countryCode = countryCode,
            countryName = supportedRegion.nameEn,
            success = failCount == 0,
            processedRegions = successCount,
            failedRegions = failCount,
        )
    }

    /**
     * 名前補完バッチ処理を実行
     * 既存SSEルート (/geo-names/enrich/all/stream) のロジックを同期的に実行
     */
    suspend fun processAllGeoNames(request: BatchGeoNamesRequest): BatchGeoNamesResult {
        val startTime = java.lang.System.currentTimeMillis()
        val countries = request.targetCountries
            ?.mapNotNull { code -> SupportedRegion.all.find { it.code2 == code } }
            ?: SupportedRegion.all

        logger.info(
            "BatchProcessingService: Starting GeoNames batch processing for ${countries.size} countries " +
                    "(batchSize=${request.batchSize}, dryRun=${request.dryRun})",
        )

        val countryResults = mutableListOf<GeoNamesCountryResult>()
        var totalSuccess = 0
        var totalFail = 0
        var totalApplied = 0
        var totalValidated = 0
        var totalSkipped = 0
        var totalFailed = 0

        countries.forEach { supportedRegion ->
            val countryStartTime = java.lang.System.currentTimeMillis()
            val countryCode = supportedRegion.code2

            logger.info("BatchProcessingService: Enriching names for ${supportedRegion.nameEn} ($countryCode)")

            try {
                var applied = 0
                var validated = 0
                var skipped = 0
                var failed = 0

                geoNameEnrichmentRepository.enrichGeoNamesAsFlow(
                    countryCode = countryCode,
                    level = null,
                    batchSize = request.batchSize,
                    dryRun = request.dryRun,
                ).collect { event ->
                    when (event) {
                        is GeoNameEnrichmentEvent.Completed -> {
                            applied = event.successCount - validated
                            // Note: validated count is tracked separately
                        }

                        is GeoNameEnrichmentEvent.ItemResult -> {
                            when (event.status) {
                                "APPLIED" -> applied++
                                "VALIDATED" -> validated++
                                "SKIPPED" -> skipped++
                                "ERROR" -> failed++
                            }
                        }

                        else -> {}
                    }
                }

                totalApplied += applied
                totalValidated += validated
                totalSkipped += skipped
                totalFailed += failed

                val isSuccess = failed == 0
                if (isSuccess) totalSuccess++ else totalFail++

                countryResults.add(
                    GeoNamesCountryResult(
                        countryCode = countryCode,
                        countryName = supportedRegion.nameEn,
                        success = isSuccess,
                        appliedCount = applied,
                        validatedCount = validated,
                        skippedCount = skipped,
                        failedCount = failed,
                        processingTimeMs = java.lang.System.currentTimeMillis() - countryStartTime,
                    ),
                )

                logger.info(
                    "BatchProcessingService: ${supportedRegion.nameEn} completed - " +
                            "applied=$applied, validated=$validated, skipped=$skipped, failed=$failed",
                )
            } catch (e: Exception) {
                totalFail++
                logger.error("BatchProcessingService: ${supportedRegion.nameEn} failed with exception", e)
                countryResults.add(
                    GeoNamesCountryResult(
                        countryCode = countryCode,
                        countryName = supportedRegion.nameEn,
                        success = false,
                        errorMessage = e.message,
                        processingTimeMs = java.lang.System.currentTimeMillis() - countryStartTime,
                    ),
                )
            }
        }

        val totalTime = java.lang.System.currentTimeMillis() - startTime
        logger.info(
            "BatchProcessingService: GeoNames batch completed - " +
                    "$totalSuccess success, $totalFail fail, ${totalTime}ms",
        )

        return BatchGeoNamesResult(
            totalCountries = countries.size,
            successCount = totalSuccess,
            failCount = totalFail,
            totalTimeMs = totalTime,
            totalApplied = totalApplied,
            totalValidated = totalValidated,
            totalSkipped = totalSkipped,
            totalFailed = totalFailed,
            countryResults = countryResults,
            executedAt = currentIsoDateTime(),
            dryRun = request.dryRun,
        )
    }

    private fun currentIsoDateTime(): String {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now().atOffset(ZoneOffset.UTC))
    }
}
