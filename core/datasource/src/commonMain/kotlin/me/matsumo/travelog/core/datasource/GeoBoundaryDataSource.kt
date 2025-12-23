package me.matsumo.travelog.core.datasource

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.common.formatter
import me.matsumo.travelog.core.model.geo.GeoBoundaryInfo
import me.matsumo.travelog.core.model.geo.GeoBoundaryLevel
import me.matsumo.travelog.core.model.geo.GeoJsonData

class GeoBoundaryDataSource(
    private val httpClient: HttpClient,
    private val geoBoundaryCacheDataSource: GeoBoundaryCacheDataSource,
    private val appSettingDataSource: AppSettingDataSource,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Fetch boundary metadata for all countries at ADM0 (country) level
     */
    suspend fun fetchAllCountries(): List<GeoBoundaryInfo> = withContext(ioDispatcher) {
        val url = "$BASE_URL/gbOpen/ALL/ADM0/"
        formatter.decodeFromString(fetchText(url))
    }

    /**
     * Fetch boundary metadata for a specific country and administrative level
     *
     * @param countryIso ISO 3166-1 alpha-3 country code (e.g., "JPN", "USA")
     * @param level Administrative level (ADM0 to ADM5)
     */
    suspend fun fetchBoundaryInfo(
        countryIso: String,
        level: GeoBoundaryLevel,
    ): GeoBoundaryInfo = withContext(ioDispatcher) {
        val url = "$BASE_URL/gbOpen/$countryIso/${level.name}/"
        formatter.decodeFromString(fetchText(url))
    }

    /**
     * Download GeoJSON data from the provided URL
     *
     * @param geoJsonUrl URL to GeoJSON file (typically from GeoBoundaryInfo.gjDownloadURL)
     */
    suspend fun downloadGeoJson(geoJsonUrl: String): GeoJsonData = withContext(ioDispatcher) {
        formatter.decodeFromString(fetchText(geoJsonUrl))
    }

    suspend fun clearCache() {
        geoBoundaryCacheDataSource.clear()
    }

    private suspend fun fetchText(url: String): String {
        val useCache = appSettingDataSource.setting.first().useGeoJsonCache
        val cacheKey = generateCacheKey(url)
        val cachedData = if (useCache) geoBoundaryCacheDataSource.load(cacheKey) else null

        return if (cachedData != null) {
            Napier.d("Cache hit for $url")
            cachedData
        } else {
            Napier.d("Cache miss for $url, downloading...")
            // application/octet-stream で返却される場合があるため bodyAsText() を使用
            val result = httpClient.get(url).bodyAsText()
            if (useCache) {
                geoBoundaryCacheDataSource.save(cacheKey, result)
            }
            result
        }
    }

    private fun generateCacheKey(url: String): String {
        val hash = url.hashCode().toString(16)
        return "gb_$hash.json"
    }

    companion object {
        private const val BASE_URL = "https://www.geoboundaries.org/api/current"
    }
}
