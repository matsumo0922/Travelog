package me.matsumo.travelog.core.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.common.retryWithBackoff
import me.matsumo.travelog.core.model.geo.WikipediaThumbnailResult

class WikipediaDataSource(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val rateLimiter = Semaphore(MAX_CONCURRENT_REQUESTS)

    suspend fun getThumbnailUrl(lang: String, title: String): String? = withContext(ioDispatcher) {
        rateLimiter.withPermit {
            delay(MIN_INTERVAL_MS)

            val encodedTitle = title.encodeURLPath()
            val url = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$encodedTitle"

            retryWithBackoff(
                maxRetries = MAX_RETRIES,
                initialDelayMs = INITIAL_DELAY_MS,
                maxDelayMs = MAX_DELAY_MS,
                retryIf = { e ->
                    when (e) {
                        is ClientRequestException -> e.response.status.value in 500..599
                        else -> false
                    }
                },
            ) {
                val result = httpClient.get(url).body<WikipediaThumbnailResult>()
                result.thumbnail?.source ?: result.originalImage?.source
            }
        }
    }

    companion object {
        private const val MAX_CONCURRENT_REQUESTS = 5
        private const val MIN_INTERVAL_MS = 100L
        private const val MAX_RETRIES = 2
        private const val INITIAL_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 5000L
    }
}
