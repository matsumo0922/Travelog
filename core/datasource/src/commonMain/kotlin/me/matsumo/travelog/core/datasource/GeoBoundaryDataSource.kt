package me.matsumo.travelog.core.datasource

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.common.formatter
import me.matsumo.travelog.core.model.geo.GeoBoundaryInfo
import me.matsumo.travelog.core.model.geo.GeoBoundaryLevel
import me.matsumo.travelog.core.model.geo.GeoJsonData

class GeoBoundaryDataSource(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun fetchBoundaryInfo(
        countryIso: String,
        level: GeoBoundaryLevel,
    ): GeoBoundaryInfo = withContext(ioDispatcher) {
        fetchJson("$BASE_URL/gbOpen/$countryIso/${level.name}/")
    }

    suspend fun downloadGeoJson(geoJsonUrl: String): GeoJsonData = withContext(ioDispatcher) {
        fetchJson(geoJsonUrl)
    }

    private suspend inline fun <reified T> fetchJson(url: String): T {
        // application/octet-stream で返却される場合があるため bodyAsText() を使用
        val result = httpClient.get(url).bodyAsText()
        return formatter.decodeFromString(result)
    }

    companion object {
        private const val BASE_URL = "https://www.geoboundaries.org/api/current"
    }
}
