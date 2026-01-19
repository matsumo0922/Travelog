package me.matsumo.travelog.core.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.geo.WikipediaThumbnailResult

class WikipediaDataSource(
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

    suspend fun getThumbnailUrl(lang: String, title: String) = withContext(ioDispatcher) {
        throttle()

        val url = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$title"
        val result = httpClient.get(url).body<WikipediaThumbnailResult>()

        result.thumbnail?.source ?: result.originalImage?.source
    }

    companion object {
        private const val MIN_INTERVAL_MS = 200L
    }
}
