package me.matsumo.travelog.core.model

import androidx.compose.runtime.Immutable
import me.matsumo.travelog.core.model.db.MapRegion
import me.matsumo.travelog.core.model.geo.GeoArea
import kotlin.time.Instant

/**
 * Represents a "moment" item for the Google Photos-like moments UI.
 * Each moment corresponds to a MapRegion with its associated GeoArea and preview images.
 */
@Immutable
data class MomentItem(
    val mapRegion: MapRegion,
    val geoArea: GeoArea,
    val previewImages: List<PreviewImage>,
    val totalImageCount: Int,
    val dateRange: DateRange?,
)

/**
 * A preview image with its signed URL and dimensions.
 */
@Immutable
data class PreviewImage(
    val id: String,
    val url: String,
    val width: Int?,
    val height: Int?,
)

/**
 * Date range for a moment, calculated from the earliest and latest taken_at timestamps.
 */
@Immutable
data class DateRange(
    val earliest: Instant,
    val latest: Instant,
)
