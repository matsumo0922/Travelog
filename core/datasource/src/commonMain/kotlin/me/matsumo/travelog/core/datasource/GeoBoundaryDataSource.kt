package me.matsumo.travelog.core.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.common.formatter
import me.matsumo.travelog.core.model.GeoBoundaryInfo
import me.matsumo.travelog.core.model.GeoBoundaryLevel
import me.matsumo.travelog.core.model.GeoJsonData

class GeoBoundaryDataSource(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Fetch boundary metadata for all countries at ADM0 (country) level
     */
    suspend fun fetchAllCountries(): List<GeoBoundaryInfo> = withContext(ioDispatcher) {
        val url = "$BASE_URL/gbOpen/ALL/ADM0/"
        httpClient.get(url).body<List<GeoBoundaryInfo>>()
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
        httpClient.get(url).body<GeoBoundaryInfo>()
    }

    /**
     * Download GeoJSON data from the provided URL
     *
     * @param geoJsonUrl URL to GeoJSON file (typically from GeoBoundaryInfo.gjDownloadURL)
     */
    suspend fun downloadGeoJson(geoJsonUrl: String): GeoJsonData = withContext(ioDispatcher) {
        // application/octet-stream で返却されるので Ktor 側で変換不可
        val response = httpClient.get(geoJsonUrl).bodyAsText()
        formatter.decodeFromString(response)
    }

    companion object {
        private const val BASE_URL = "https://www.geoboundaries.org/api/current"
    }
}
