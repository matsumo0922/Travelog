package me.matsumo.travelog.core.usecase

import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Instant

actual suspend fun extractImageMetadata(file: PlatformFile): ImageMetadata? {
    return runCatching {
        val bytes = file.readBytes()

        // Get image dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return@runCatching null
        }

        // Extract EXIF data
        val inputStream = ByteArrayInputStream(bytes)
        val exifInterface = ExifInterface(inputStream)

        // Parse taken date
        val takenAt = parseTakenDate(exifInterface)

        // Parse GPS coordinates
        val (latitude, longitude) = parseGpsCoordinates(exifInterface)

        // Build EXIF JSON object
        val exifJson = buildExifJson(exifInterface)

        ImageMetadata(
            width = options.outWidth,
            height = options.outHeight,
            takenAt = takenAt,
            takenLat = latitude,
            takenLng = longitude,
            exif = exifJson,
        )
    }.getOrNull()
}

private fun parseTakenDate(exif: ExifInterface): Instant? {
    // Try multiple date tags
    val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
        ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
        ?: return null

    return runCatching {
        val formatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        val date = formatter.parse(dateString) ?: return@runCatching null
        Instant.fromEpochMilliseconds(date.time)
    }.getOrNull()
}

private fun parseGpsCoordinates(exif: ExifInterface): Pair<Double?, Double?> {
    val latLong = FloatArray(2)
    return if (exif.getLatLong(latLong)) {
        latLong[0].toDouble() to latLong[1].toDouble()
    } else {
        null to null
    }
}

private fun buildExifJson(exif: ExifInterface): JsonObject {
    return buildJsonObject {
        // Camera info
        exif.getAttribute(ExifInterface.TAG_MAKE)?.let { put("make", JsonPrimitive(it)) }
        exif.getAttribute(ExifInterface.TAG_MODEL)?.let { put("model", JsonPrimitive(it)) }

        // Lens info
        exif.getAttribute(ExifInterface.TAG_LENS_MAKE)?.let { put("lensMake", JsonPrimitive(it)) }
        exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.let { put("lensModel", JsonPrimitive(it)) }

        // Shooting settings
        exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { put("fNumber", JsonPrimitive(it)) }
        exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { put("exposureTime", JsonPrimitive(it)) }
        exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { put("iso", JsonPrimitive(it)) }
        exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { put("focalLength", JsonPrimitive(it)) }
        exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)?.let { put("focalLength35mm", JsonPrimitive(it)) }

        // Flash
        exif.getAttribute(ExifInterface.TAG_FLASH)?.let { put("flash", JsonPrimitive(it)) }

        // Orientation
        exif.getAttribute(ExifInterface.TAG_ORIENTATION)?.let { put("orientation", JsonPrimitive(it)) }

        // Date/Time
        exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { put("dateTimeOriginal", JsonPrimitive(it)) }

        // GPS
        exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)?.let { put("gpsLatitude", JsonPrimitive(it)) }
        exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)?.let { put("gpsLatitudeRef", JsonPrimitive(it)) }
        exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)?.let { put("gpsLongitude", JsonPrimitive(it)) }
        exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)?.let { put("gpsLongitudeRef", JsonPrimitive(it)) }
        exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)?.let { put("gpsAltitude", JsonPrimitive(it)) }
        exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF)?.let { put("gpsAltitudeRef", JsonPrimitive(it)) }

        // Software
        exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.let { put("software", JsonPrimitive(it)) }
    }
}
