package datasource

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import me.matsumo.travelog.core.common.retryWithBackoff
import model.GeoNameBatchResult
import model.MissingNameArea

class GeminiDataSource(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val rateLimiter = Semaphore(MAX_CONCURRENT_REQUESTS)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * バッチで地名補完を実行する
     */
    suspend fun enrichGeoNames(areas: List<MissingNameArea>): GeoNameBatchResult = withContext(ioDispatcher) {
        rateLimiter.withPermit {
            delay(MIN_INTERVAL_MS)

            val prompt = buildPrompt(areas)
            val requestBody = buildRequestBody(prompt)

            retryWithBackoff(
                maxRetries = MAX_RETRIES,
                initialDelayMs = INITIAL_DELAY_MS,
                maxDelayMs = MAX_DELAY_MS,
                retryIf = { true },
            ) {
                val response = httpClient.post("$BASE_URL?key=$apiKey") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }.body<JsonObject>()

                parseResponse(response, areas)
            }
        }
    }

    /**
     * レート制限エラーを表す例外
     */
    class RateLimitException(message: String) : Exception(message)

    private fun buildPrompt(areas: List<MissingNameArea>): String {
        val areasJson = areas.joinToString(",\n") { area ->
            """  {
    "adm_id": "${area.admId}",
    "default_name": "${area.name}",
    "country_code": "${area.countryCode}",
    "level": ${area.level},
    "parent_region": ${area.parentName?.let { "\"$it\"" } ?: "null"},
    "current_name_en": ${area.nameEn?.let { "\"$it\"" } ?: "null"},
    "current_name_ja": ${area.nameJa?.let { "\"$it\"" } ?: "null"}
  }"""
        }

        return """You are a geographic naming expert specializing in official administrative region names.

## Task
For each administrative region below, provide the official English name and native language name.

## Input Data
[
$areasJson
]

## Rules for Japanese regions (country_code: JP):
1. name_ja (Japanese names):
   - Use official kanji/hiragana as registered
   - ADM1 (都道府県) must end with: 都, 道, 府, or 県
   - ADM2 (市区町村) must end with: 市, 町, 村, 区, or 郡
   - Use the full official name (e.g., "東京都", "大阪府", "忠岡町")

2. name_en (English names):
   - Use modified Hepburn romanization
   - ADM1: Include suffix (e.g., "Tokyo Metropolis", "Osaka Prefecture", "Hokkaido")
   - ADM2: Include suffix (e.g., "Tadaoka Town", "Osaka City")

## Rules for Korean regions (country_code: KR):
1. name_ja: Use standard Japanese katakana transliteration (e.g., "ソウル特別市")
2. name_en: Use official romanization (e.g., "Seoul", "Busan")

## Rules for Taiwanese regions (country_code: TW):
1. name_ja: Use traditional Chinese characters or Japanese reading (e.g., "台北市")
2. name_en: Use official romanization (e.g., "Taipei City", "New Taipei City")

## Rules for Chinese regions (country_code: CN):
1. name_ja: Use Japanese reading of Chinese characters (e.g., "北京市" → "ペキン市")
2. name_en: Use Pinyin romanization (e.g., "Beijing", "Shanghai")

## Rules for US regions (country_code: US):
1. name_ja: Use standard Japanese katakana transliteration (e.g., "カリフォルニア州")
2. name_en: Use official English name (e.g., "California", "New York")

## Rules for German regions (country_code: DE):
1. name_ja: Use standard Japanese katakana transliteration (e.g., "バイエルン州")
2. name_en: Use the official German name or common English equivalent

## Rules for British regions (country_code: GB):
1. name_ja: Use standard Japanese katakana transliteration (e.g., "イングランド")
2. name_en: Use the official English name

## Rules for French regions (country_code: FR):
1. name_ja: Use standard Japanese katakana transliteration (e.g., "イル＝ド＝フランス")
2. name_en: Use the official French name or common English equivalent

## Confidence scoring:
- 1.0: Certain (official name found)
- 0.8: High confidence (well-known region)
- 0.5: Medium (educated guess based on patterns)
- 0.3: Low (uncertain)

## Output Format
Return a JSON object with this exact structure:
{
  "results": [
    {
      "adm_id": "string (same as input)",
      "name_en": "string",
      "name_ja": "string",
      "confidence": number,
      "reasoning": "brief explanation (optional)"
    }
  ]
}

Only output the JSON, no additional text."""
    }

    private fun buildRequestBody(prompt: String): JsonObject {
        return buildJsonObject {
            putJsonArray("contents") {
                add(
                    buildJsonObject {
                        putJsonArray("parts") {
                            add(
                                buildJsonObject {
                                    put("text", prompt)
                                },
                            )
                        }
                    },
                )
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                putJsonObject("responseSchema") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("results") {
                            put("type", "ARRAY")
                            putJsonObject("items") {
                                put("type", "OBJECT")
                                putJsonObject("properties") {
                                    putJsonObject("adm_id") {
                                        put("type", "STRING")
                                    }
                                    putJsonObject("name_en") {
                                        put("type", "STRING")
                                    }
                                    putJsonObject("name_ja") {
                                        put("type", "STRING")
                                    }
                                    putJsonObject("confidence") {
                                        put("type", "NUMBER")
                                    }
                                    putJsonObject("reasoning") {
                                        put("type", "STRING")
                                    }
                                }
                                putJsonArray("required") {
                                    add(JsonPrimitive("adm_id"))
                                    add(JsonPrimitive("name_en"))
                                    add(JsonPrimitive("name_ja"))
                                    add(JsonPrimitive("confidence"))
                                }
                            }
                        }
                    }
                    putJsonArray("required") {
                        add(JsonPrimitive("results"))
                    }
                }
            }
        }
    }

    private fun parseResponse(response: JsonObject, originalAreas: List<MissingNameArea>): GeoNameBatchResult {
        // レスポンス全体をログに出力（デバッグ用）
        Napier.d(tag = TAG) { "Gemini API Response: $response" }

        // エラーチェック
        val error = response["error"]?.jsonObject
        if (error != null) {
            val errorMessage = error["message"]?.jsonPrimitive?.content ?: "Unknown error"
            val errorCode = error["code"]?.jsonPrimitive?.content ?: "Unknown code"
            Napier.e(tag = TAG) { "Gemini API Error: code=$errorCode, message=$errorMessage" }

            // 429 レート制限エラーの場合はリトライ可能な例外を投げる
            if (errorCode == "429" || errorMessage.contains("Resource exhausted", ignoreCase = true)) {
                throw RateLimitException("Rate limit exceeded: $errorMessage")
            }
            error("Gemini API Error: $errorMessage (code: $errorCode)")
        }

        // promptFeedback のチェック（コンテンツがブロックされた場合）
        val promptFeedback = response["promptFeedback"]?.jsonObject
        if (promptFeedback != null) {
            val blockReason = promptFeedback["blockReason"]?.jsonPrimitive?.content
            if (blockReason != null) {
                Napier.e(tag = TAG) { "Prompt blocked: reason=$blockReason, feedback=$promptFeedback" }
                error("Prompt blocked by Gemini: $blockReason")
            }
        }

        val candidates = response["candidates"]?.jsonArray
        if (candidates == null || candidates.isEmpty()) {
            // candidates がない場合、レスポンス全体をエラーメッセージに含める
            val responsePreview = response.toString().take(500)
            Napier.e(tag = TAG) { "No candidates in response. Response preview: $responsePreview" }
            error("No candidates in response. Response: $responsePreview")
        }

        val firstCandidate = candidates.firstOrNull()?.jsonObject
            ?: error("Empty candidates array")

        // finishReason のチェック
        val finishReason = firstCandidate["finishReason"]?.jsonPrimitive?.content
        if (finishReason != null && finishReason != "STOP") {
            Napier.w(tag = TAG) { "Unexpected finishReason: $finishReason" }
            if (finishReason == "SAFETY") {
                val safetyRatings = firstCandidate["safetyRatings"]?.jsonArray
                Napier.e(tag = TAG) { "Content blocked due to safety. Ratings: $safetyRatings" }
                error("Content blocked due to safety filters: $safetyRatings")
            }
        }

        val content = firstCandidate["content"]?.jsonObject
            ?: error("No content in candidate")

        val parts = content["parts"]?.jsonArray
            ?: error("No parts in content")

        val firstPart = parts.firstOrNull()?.jsonObject
            ?: error("Empty parts array")

        val text = firstPart["text"]?.jsonPrimitive?.content
            ?: error("No text in part")

        Napier.d(tag = TAG) { "Parsed text: $text" }

        val parsedResult = json.decodeFromString<GeoNameBatchResult>(text)

        // マッピングを検証（入力のadm_idと出力のadm_idが一致しているか）
        val inputAdmIds = originalAreas.map { it.admId }.toSet()
        val validResults = parsedResult.results.filter { it.admId in inputAdmIds }

        return GeoNameBatchResult(results = validResults)
    }

    companion object {
        private const val TAG = "GeminiDataSource"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        private const val MAX_CONCURRENT_REQUESTS = 5
        private const val MIN_INTERVAL_MS = 3000L // 3秒間隔
        private const val MAX_RETRIES = 5
        private const val INITIAL_DELAY_MS = 5000L // 5秒
        private const val MAX_DELAY_MS = 30000L // 30秒
    }
}
