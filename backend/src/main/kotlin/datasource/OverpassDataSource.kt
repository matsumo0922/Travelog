package datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.matsumo.travelog.core.common.retryWithBackoff
import me.matsumo.travelog.core.model.geo.OverpassResult

class ContentTypeException(
    message: String,
    val actualContentType: ContentType?,
) : Exception(message)

class OverpassDataSource(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun getAdmins(osmId: Long, adminLevel: Int): OverpassResult = withContext(ioDispatcher) {
        val query = """
            [out:json][timeout:60];
             area(${osmId + 3_600_000_000})->.searchArea;
             (
               .searchArea;
               relation["boundary"="administrative"]["admin_level"~"${if (adminLevel <= 4) "4" else "7|8"}"](area.searchArea);
             );
             out tags center;
        """.trimIndent()

        executeQuery(query)
    }

    /**
     * Get administrative boundaries by ISO 3166-2 code.
     * This allows parallel processing by bypassing the Nominatim rate limit.
     *
     * @param isoCode ISO 3166-2 code (e.g., "JP-13" for Tokyo)
     */
    suspend fun getAdminsByIso(isoCode: String): OverpassResult = withContext(ioDispatcher) {
        val query = """
            [out:json][timeout:60];
            area["ISO3166-2"="$isoCode"]->.searchArea;
            (
              .searchArea;
              relation["boundary"="administrative"]["admin_level"~"7|8"](area.searchArea);
            );
            out tags center;
        """.trimIndent()

        executeQuery(query)
    }

    private suspend fun executeQuery(query: String): OverpassResult {
        return retryWithBackoff(
            maxRetries = MAX_RETRIES,
            initialDelayMs = INITIAL_DELAY_MS,
            maxDelayMs = MAX_DELAY_MS,
            retryIf = { it is ContentTypeException },
        ) {
            val response: HttpResponse = httpClient.post(BASE_URL) {
                setBody(query)
            }

            val contentType = response.contentType()
            if (contentType != null && !contentType.match(ContentType.Application.Json)) {
                throw ContentTypeException(
                    message = "Expected JSON but received $contentType from Overpass API",
                    actualContentType = contentType,
                )
            }

            response.body()
        }
    }

    companion object {
        private const val BASE_URL = "https://overpass.kumi.systems/api/interpreter"
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 2000L
        private const val MAX_DELAY_MS = 15000L
    }
}
