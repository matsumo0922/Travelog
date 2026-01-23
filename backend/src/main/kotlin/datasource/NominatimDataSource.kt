package datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.geo.NominatimResult

class NominatimDataSource(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    // Nominatim の公式レートリミット: 1 req/sec
    // Semaphore(1) で同時実行を1つに制限
    private val rateLimiter = Semaphore(1)

    suspend fun search(query: String) = withContext(ioDispatcher) {
        rateLimiter.withPermit {
            delay(MIN_INTERVAL_MS)

            httpClient.get("$BASE_URL/search") {
                parameter("q", query)
                parameter("format", "jsonv2")
                parameter("limit", "1")
            }.body<List<NominatimResult>>().first()
        }
    }

    companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org"
        private const val MIN_INTERVAL_MS = 1200L
    }
}
