package me.matsumo.travelog.core.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.geo.NominatimResult

class NominatimDataSource(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val requestMutex = Mutex()
    private var isFirstRequest = true

    private suspend fun throttle() {
        requestMutex.withLock {
            if (!isFirstRequest) delay(MIN_INTERVAL_MS)
            isFirstRequest = false
        }
    }

    suspend fun search(query: String) = withContext(ioDispatcher) {
        throttle()

        httpClient.get("$BASE_URL/search") {
            parameter("q", query)
            parameter("format", "jsonv2")
            parameter("limit", "1")
        }.body<List<NominatimResult>>().first()
    }

    companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org"
        private const val MIN_INTERVAL_MS = 1200L
    }
}