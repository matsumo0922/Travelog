package route

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import model.BatchGeoJsonRequest
import model.BatchGeoNamesRequest
import org.koin.ktor.ext.inject
import service.BatchProcessingService

/**
 * cron用バッチ処理APIルート
 *
 * 認証: Bearer token (環境変数 BATCH_API_KEY)
 *
 * エンドポイント:
 * - POST /api/batch/geojson/all - GeoJSONバッチ処理
 * - POST /api/batch/geo-names/all - 名前補完バッチ処理
 */
fun Application.batchApiRoute() {
    val batchProcessingService by inject<BatchProcessingService>()

    routing {
        route("/api/batch") {
            authenticate("auth-api-key") {
                /**
                 * GeoJSONバッチ処理
                 *
                 * リクエスト:
                 * {
                 *   "targetCountries": ["JP", "US"]  // optional, null = 全国
                 * }
                 *
                 * レスポンス:
                 * {
                 *   "totalCountries": 16,
                 *   "successCount": 16,
                 *   "failCount": 0,
                 *   "totalTimeMs": 123456,
                 *   "countryResults": [...],
                 *   "executedAt": "2026-01-21T10:00:00Z"
                 * }
                 */
                post("/geojson/all") {
                    val request = runCatching { call.receive<BatchGeoJsonRequest>() }
                        .getOrElse { BatchGeoJsonRequest() }

                    val result = batchProcessingService.processAllGeoJson(request)
                    call.respond(HttpStatusCode.OK, result)
                }

                /**
                 * 名前補完バッチ処理
                 *
                 * リクエスト:
                 * {
                 *   "targetCountries": ["JP"],  // optional, null = 全国
                 *   "batchSize": 10,            // default: 10
                 *   "dryRun": false             // default: false
                 * }
                 *
                 * レスポンス:
                 * {
                 *   "totalCountries": 16,
                 *   "successCount": 16,
                 *   "failCount": 0,
                 *   "totalTimeMs": 123456,
                 *   "totalApplied": 100,
                 *   "totalValidated": 50,
                 *   "totalSkipped": 10,
                 *   "totalFailed": 0,
                 *   "countryResults": [...],
                 *   "executedAt": "2026-01-21T10:00:00Z",
                 *   "dryRun": false
                 * }
                 */
                post("/geo-names/all") {
                    val request = runCatching { call.receive<BatchGeoNamesRequest>() }
                        .getOrElse { BatchGeoNamesRequest() }

                    val result = batchProcessingService.processAllGeoNames(request)
                    call.respond(HttpStatusCode.OK, result)
                }
            }
        }
    }
}
