package me.matsumo.travelog.core.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.geo.OverpassResult

class OverpassDataSource(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun getAdmins(osmId: String): OverpassResult = withContext(ioDispatcher) {
        val query = """
            [out:json];
             area($osmId)->.searchArea;
             relation["admin_level"~"7|8"](area.searchArea);
             out tags;
        """.trimIndent()

        httpClient.get("$BASE_URL?data=$query").body()
    }

    companion object {
        private const val BASE_URL = "https://overpass-api.de/api/interpreter"
    }
}