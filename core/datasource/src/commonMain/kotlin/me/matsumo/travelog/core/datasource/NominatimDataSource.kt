package me.matsumo.travelog.core.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.parameters
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.geo.NominatimResult

class NominatimDataSource(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun search(query: String) = withContext(ioDispatcher) {
        httpClient.get("$BASE_URL/search") {
            parameters {
                append("q", query)
                append("format", "jsonv2")
                append("polygon_geojson", "1")
                append("polygon_threshold", "0.01")
            }
        }.body<List<NominatimResult>>().first()
    }

    companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org/"
    }
}