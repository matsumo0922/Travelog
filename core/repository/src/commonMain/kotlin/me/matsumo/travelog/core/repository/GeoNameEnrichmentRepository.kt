package me.matsumo.travelog.core.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import me.matsumo.travelog.core.datasource.GeminiDataSource
import me.matsumo.travelog.core.datasource.api.GeoAreaApi
import me.matsumo.travelog.core.datasource.api.MissingNamesCount
import me.matsumo.travelog.core.datasource.api.NameUpdateItem
import me.matsumo.travelog.core.model.gemini.EnrichmentStatus
import me.matsumo.travelog.core.model.gemini.GeoNameEnrichmentEvent
import me.matsumo.travelog.core.model.gemini.GeoNameEnrichmentItem
import me.matsumo.travelog.core.model.gemini.MissingNameArea
import kotlin.time.TimeSource

class GeoNameEnrichmentRepository(
    private val geoAreaApi: GeoAreaApi,
    private val geminiDataSource: GeminiDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Get areas with missing names for a country (optionally filtered by level).
     */
    suspend fun getAreasWithMissingNames(
        countryCode: String,
        level: Int? = null,
    ): List<MissingNameArea> {
        val dtos = geoAreaApi.fetchAreasWithMissingNames(countryCode, level)
        val parentIds = dtos.mapNotNull { it.parentId }.distinct()
        val parentMap = parentIds.associateWith { parentId ->
            geoAreaApi.fetchAreaById(parentId)?.name
        }

        return dtos.map { dto ->
            MissingNameArea(
                id = dto.id ?: "",
                admId = dto.admId,
                countryCode = dto.countryCode,
                level = dto.level,
                name = dto.name,
                nameEn = dto.nameEn,
                nameJa = dto.nameJa,
                parentName = dto.parentId?.let { parentMap[it] },
            )
        }
    }

    /**
     * Get count of areas with missing names.
     */
    suspend fun getMissingNamesCount(countryCode: String, level: Int? = null): MissingNamesCount {
        return geoAreaApi.getMissingNamesCount(countryCode, level)
    }

    /**
     * Enrich geo names using Gemini API and emit progress events.
     *
     * @param countryCode The country code to process
     * @param level The administrative level to process (null for all levels)
     * @param batchSize Number of areas to process in each batch
     * @param dryRun If true, don't actually update the database
     */
    fun enrichGeoNamesAsFlow(
        countryCode: String,
        level: Int? = null,
        batchSize: Int = BATCH_SIZE,
        dryRun: Boolean = false,
    ): Flow<GeoNameEnrichmentEvent> = flow {
        val startMark = TimeSource.Monotonic.markNow()
        var totalProcessed = 0
        var appliedCount = 0
        var validatedCount = 0
        var skippedCount = 0
        var failedCount = 0

        try {
            // Fetch all areas with missing names
            val areas = getAreasWithMissingNames(countryCode, level)

            if (areas.isEmpty()) {
                emit(
                    GeoNameEnrichmentEvent.Completed(
                        totalProcessed = 0,
                        successCount = 0,
                        appliedCount = 0,
                        validatedCount = 0,
                        skippedCount = 0,
                        failedCount = 0,
                        elapsedMs = startMark.elapsedNow().inWholeMilliseconds,
                    ),
                )
                return@flow
            }

            // Emit started event
            emit(
                GeoNameEnrichmentEvent.Started(
                    totalCount = areas.size,
                    countryCode = countryCode,
                    level = level,
                ),
            )

            // Group areas by parent for better context
            val batches = areas
                .groupBy { it.parentName ?: "unknown" }
                .flatMap { (_, groupedAreas) ->
                    groupedAreas.chunked(batchSize)
                }

            batches.forEachIndexed { batchIndex, batch ->
                try {
                    // Call Gemini API
                    val result = geminiDataSource.enrichGeoNames(batch)

                    // Process results
                    val updates = mutableListOf<NameUpdateItem>()
                    var batchApplied = 0
                    var batchValidated = 0
                    var batchSkipped = 0

                    result.results.forEach { item ->
                        val area = batch.find { it.admId == item.admId }
                        if (area != null) {
                            val status = determineStatus(item, countryCode, area.level)

                            emit(
                                GeoNameEnrichmentEvent.ItemResult(
                                    areaId = area.id,
                                    admId = area.admId,
                                    originalName = area.name,
                                    nameEn = item.nameEn,
                                    nameJa = item.nameJa,
                                    confidence = item.confidence,
                                    status = status.name,
                                    reasoning = item.reasoning,
                                ),
                            )

                            when (status) {
                                EnrichmentStatus.APPLIED -> {
                                    batchApplied++
                                    updates.add(
                                        NameUpdateItem(
                                            areaId = area.id,
                                            nameEn = item.nameEn.takeIf { area.nameEn == null },
                                            nameJa = item.nameJa.takeIf { area.nameJa == null },
                                        ),
                                    )
                                }

                                EnrichmentStatus.VALIDATED -> {
                                    batchValidated++
                                    updates.add(
                                        NameUpdateItem(
                                            areaId = area.id,
                                            nameEn = item.nameEn.takeIf { area.nameEn == null },
                                            nameJa = item.nameJa.takeIf { area.nameJa == null },
                                        ),
                                    )
                                }

                                EnrichmentStatus.SKIPPED -> batchSkipped++
                                EnrichmentStatus.ERROR -> failedCount++
                            }
                        }
                    }

                    // Apply updates to database
                    if (!dryRun && updates.isNotEmpty()) {
                        geoAreaApi.updateAreaNamesBatch(updates)
                    }

                    appliedCount += batchApplied
                    validatedCount += batchValidated
                    skippedCount += batchSkipped
                    totalProcessed += batch.size

                    emit(
                        GeoNameEnrichmentEvent.BatchProcessed(
                            batchIndex = batchIndex + 1,
                            totalBatches = batches.size,
                            processedCount = totalProcessed,
                            appliedCount = appliedCount,
                            validatedCount = validatedCount,
                            skippedCount = skippedCount,
                        ),
                    )
                } catch (e: Exception) {
                    failedCount += batch.size
                    emit(
                        GeoNameEnrichmentEvent.Error(
                            message = "Batch ${batchIndex + 1} failed: ${e.message}",
                        ),
                    )
                }
            }

            emit(
                GeoNameEnrichmentEvent.Completed(
                    totalProcessed = totalProcessed,
                    successCount = appliedCount + validatedCount,
                    appliedCount = appliedCount,
                    validatedCount = validatedCount,
                    skippedCount = skippedCount,
                    failedCount = failedCount,
                    elapsedMs = startMark.elapsedNow().inWholeMilliseconds,
                ),
            )
        } catch (e: Exception) {
            emit(GeoNameEnrichmentEvent.Error(message = "Processing failed: ${e.message}"))
        }
    }.flowOn(ioDispatcher)

    /**
     * Determine the status of an enrichment result based on confidence and validation.
     */
    private fun determineStatus(item: GeoNameEnrichmentItem, countryCode: String, level: Int): EnrichmentStatus {
        return when {
            item.confidence >= HIGH_CONFIDENCE_THRESHOLD -> EnrichmentStatus.APPLIED
            item.confidence >= MEDIUM_CONFIDENCE_THRESHOLD -> {
                if (validateNamePattern(item, countryCode, level)) {
                    EnrichmentStatus.VALIDATED
                } else {
                    EnrichmentStatus.SKIPPED
                }
            }

            else -> EnrichmentStatus.SKIPPED
        }
    }

    /**
     * Validate name pattern based on country-specific rules.
     */
    private fun validateNamePattern(item: GeoNameEnrichmentItem, countryCode: String, level: Int): Boolean {
        return when (countryCode) {
            "JP" -> validateJapanesePattern(item.nameJa, level)
            else -> true // For other countries, trust medium confidence
        }
    }

    /**
     * Validate Japanese administrative region name pattern.
     * ADM1 (都道府県) should end with 都, 道, 府, or 県
     * ADM2 (市区町村) should end with 市, 町, 村, 区, or 郡
     */
    private fun validateJapanesePattern(nameJa: String, level: Int): Boolean {
        val validSuffixes = when (level) {
            1 -> listOf("都", "道", "府", "県")
            2 -> listOf("市", "町", "村", "区", "郡")
            else -> return true // For other levels, trust the result
        }
        return validSuffixes.any { nameJa.endsWith(it) }
    }

    companion object {
        private const val BATCH_SIZE = 10
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.8
        private const val MEDIUM_CONFIDENCE_THRESHOLD = 0.5
    }
}
