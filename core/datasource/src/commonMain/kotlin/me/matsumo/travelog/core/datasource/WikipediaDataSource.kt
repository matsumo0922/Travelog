package me.matsumo.travelog.core.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.model.geo.WikipediaThumbnailResult

class WikipediaDataSource(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun getThumbnailUrl(lang: String, title: String) = withContext(ioDispatcher) {
        val url = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$title"
        val result = httpClient.get(url).body<WikipediaThumbnailResult>()

        result.originalImage?.source ?: result.thumbnail?.source
    }
}
