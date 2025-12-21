package me.matsumo.travelog.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.matsumo.travelog.core.datasource.GeoBoundaryDataSource
import me.matsumo.travelog.core.model.GeoBoundaryInfo
import me.matsumo.travelog.core.model.GeoBoundaryLevel
import me.matsumo.travelog.core.model.GeoJsonData

class GeoBoundaryRepository(
    private val dataSource: GeoBoundaryDataSource,
    private val appSettingRepository: AppSettingRepository,
) {
    val isCacheEnabled: Flow<Boolean> = appSettingRepository.setting.map { it.useGeoJsonCache }

    suspend fun setCacheEnabled(enabled: Boolean) {
        appSettingRepository.setUseGeoJsonCache(enabled)
    }

    suspend fun clearCache() {
        dataSource.clearCache()
    }
    /**
     * Get all countries' boundary metadata at ADM0 level
     */
    suspend fun getAllCountries(): List<GeoBoundaryInfo> {
        return dataSource.fetchAllCountries()
    }

    /**
     * Get boundary metadata for a specific country and administrative level
     *
     * @param countryIso ISO 3166-1 alpha-3 country code (e.g., "JPN", "USA")
     * @param level Administrative level (ADM0 to ADM5)
     */
    suspend fun getBoundaryInfo(
        countryIso: String,
        level: GeoBoundaryLevel,
    ): GeoBoundaryInfo {
        return dataSource.fetchBoundaryInfo(countryIso, level)
    }

    /**
     * Download GeoJSON polygon data
     *
     * @param boundaryInfo Boundary metadata containing the GeoJSON download URL
     */
    suspend fun downloadGeoJson(boundaryInfo: GeoBoundaryInfo): GeoJsonData {
        val geoJsonUrl = boundaryInfo.gjDownloadURL ?: error("gjDownloadURL is null.")
        return dataSource.downloadGeoJson(geoJsonUrl)
    }

    /**
     * Download GeoJSON polygon data from a direct URL
     *
     * @param geoJsonUrl URL to GeoJSON file
     */
    suspend fun downloadGeoJsonFromUrl(geoJsonUrl: String): GeoJsonData {
        return dataSource.downloadGeoJson(geoJsonUrl)
    }
}
