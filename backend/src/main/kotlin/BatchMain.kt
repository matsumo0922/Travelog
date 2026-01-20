import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import me.matsumo.travelog.core.common.di.commonModule
import me.matsumo.travelog.core.datasource.GeminiDataSource
import me.matsumo.travelog.core.datasource.api.GeoAreaApi
import me.matsumo.travelog.core.datasource.di.dataSourceModule
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.GeoBoundaryRepository
import me.matsumo.travelog.core.repository.GeoNameEnrichmentRepository
import me.matsumo.travelog.core.repository.di.repositoryModule
import model.BatchGeoJsonRequest
import model.BatchGeoNamesRequest
import org.koin.core.context.startKoin
import org.koin.dsl.module
import service.BatchProcessingService

/**
 * Heroku one-off dyno用バッチ処理エントリーポイント
 *
 * 使用方法:
 *   heroku run java -cp backend/build/libs/backend-all.jar BatchMainKt geojson
 *   heroku run java -cp backend/build/libs/backend-all.jar BatchMainKt geo-names
 *   heroku run java -cp backend/build/libs/backend-all.jar BatchMainKt geojson JP,US
 */
fun main(args: Array<String>) = runBlocking {
    Napier.base(DebugAntilog())

    val mode = args.getOrNull(0) ?: "geojson"
    val targetCountries = args.getOrNull(1)?.split(",")?.map { it.trim() }

    println("=".repeat(60))
    println("Batch Processing Started")
    println("Mode: $mode")
    println("Target Countries: ${targetCountries ?: "ALL"}")
    println("=".repeat(60))

    val koin = initKoin()
    val service = koin.get<BatchProcessingService>()

    when (mode) {
        "geojson" -> {
            // Phase 1: GeoJSON Processing
            val geoJsonRequest = BatchGeoJsonRequest(targetCountries = targetCountries)
            val geoJsonResult = service.processAllGeoJson(geoJsonRequest)

            println()
            println("=".repeat(60))
            println("Phase 1: GeoJSON Batch Processing Completed")
            println("=".repeat(60))
            println("Total Countries: ${geoJsonResult.totalCountries}")
            println("Success: ${geoJsonResult.successCount}")
            println("Failed: ${geoJsonResult.failCount}")
            println("Total Time: ${geoJsonResult.totalTimeMs / 1000}s")
            println()

            geoJsonResult.countryResults.forEach { country ->
                val status = if (country.success) "✓" else "✗"
                println(
                    "  $status ${country.countryName} (${country.countryCode}): " +
                            "${country.processedRegions} regions, ${country.processingTimeMs}ms",
                )
                country.errorMessage?.let { println("    Error: $it") }
            }

            // Phase 2: Name Enrichment (auto-run after GeoJSON)
            println()
            println("=".repeat(60))
            println("Phase 2: Starting Name Enrichment...")
            println("=".repeat(60))

            val batchSize = args.getOrNull(2)?.toIntOrNull() ?: 10
            val geoNamesRequest = BatchGeoNamesRequest(
                targetCountries = targetCountries,
                batchSize = batchSize,
                dryRun = false,
            )
            val geoNamesResult = service.processAllGeoNames(geoNamesRequest)

            println()
            println("=".repeat(60))
            println("Phase 2: Name Enrichment Completed")
            println("=".repeat(60))
            println("Total Countries: ${geoNamesResult.totalCountries}")
            println("Success: ${geoNamesResult.successCount}")
            println("Failed: ${geoNamesResult.failCount}")
            println("Total Time: ${geoNamesResult.totalTimeMs / 1000}s")
            println()
            println("Summary:")
            println("  Applied: ${geoNamesResult.totalApplied}")
            println("  Validated: ${geoNamesResult.totalValidated}")
            println("  Skipped: ${geoNamesResult.totalSkipped}")
            println("  Failed: ${geoNamesResult.totalFailed}")
            println()

            geoNamesResult.countryResults.forEach { country ->
                val status = if (country.success) "✓" else "✗"
                println(
                    "  $status ${country.countryName}: " +
                            "applied=${country.appliedCount}, validated=${country.validatedCount}, " +
                            "skipped=${country.skippedCount}, failed=${country.failedCount}",
                )
                country.errorMessage?.let { println("    Error: $it") }
            }

            // Final Summary
            println()
            println("=".repeat(60))
            println("All Processing Completed")
            println("=".repeat(60))
            val totalTime = geoJsonResult.totalTimeMs + geoNamesResult.totalTimeMs
            println("Total Time: ${totalTime / 1000}s (${totalTime / 1000 / 60} min)")
            println("Executed At: ${geoNamesResult.executedAt}")
        }

        "geo-names" -> {
            val batchSize = args.getOrNull(2)?.toIntOrNull() ?: 10
            val dryRun = args.getOrNull(3)?.toBoolean() ?: false

            val request = BatchGeoNamesRequest(
                targetCountries = targetCountries,
                batchSize = batchSize,
                dryRun = dryRun,
            )
            val result = service.processAllGeoNames(request)

            println()
            println("=".repeat(60))
            println("GeoNames Batch Processing Completed${if (dryRun) " (DRY RUN)" else ""}")
            println("=".repeat(60))
            println("Total Countries: ${result.totalCountries}")
            println("Success: ${result.successCount}")
            println("Failed: ${result.failCount}")
            println("Total Time: ${result.totalTimeMs / 1000}s")
            println()
            println("Summary:")
            println("  Applied: ${result.totalApplied}")
            println("  Validated: ${result.totalValidated}")
            println("  Skipped: ${result.totalSkipped}")
            println("  Failed: ${result.totalFailed}")
            println()

            result.countryResults.forEach { country ->
                val status = if (country.success) "✓" else "✗"
                println(
                    "  $status ${country.countryName}: " +
                            "applied=${country.appliedCount}, validated=${country.validatedCount}, " +
                            "skipped=${country.skippedCount}, failed=${country.failedCount}",
                )
                country.errorMessage?.let { println("    Error: $it") }
            }
        }

        else -> {
            println("Unknown mode: $mode")
            println("Usage: BatchMainKt <geojson|geo-names> [target_countries] [batch_size] [dry_run]")
            println("Examples:")
            println("  BatchMainKt geojson")
            println("  BatchMainKt geojson JP,US")
            println("  BatchMainKt geo-names")
            println("  BatchMainKt geo-names JP 10 false")
        }
    }

    println()
    println("Done!")
}

private val jsonFormatter = Json {
    isLenient = true
    prettyPrint = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    explicitNulls = false
}

private fun initKoin(): org.koin.core.Koin {
    val supabaseClient = createSupabaseClient(
        supabaseUrl = System.getenv("SUPABASE_URL") ?: error("SUPABASE_URL not set"),
        supabaseKey = System.getenv("SUPABASE_KEY") ?: error("SUPABASE_KEY not set"),
    ) {
        defaultSerializer = KotlinXSerializer(jsonFormatter)
        install(Postgrest)
    }

    val geminiApiKey = System.getenv("GEMINI_API_KEY") ?: ""

    val app = startKoin {
        modules(
            module {
                single<SupabaseClient> { supabaseClient }
            },
            commonModule,
            dataSourceModule,
            repositoryModule,
            module {
                single { GeminiDataSource(get<HttpClient>(), geminiApiKey) }
                single { GeoNameEnrichmentRepository(get<GeoAreaApi>(), get<GeminiDataSource>()) }
                single {
                    BatchProcessingService(
                        geoBoundaryRepository = get<GeoBoundaryRepository>(),
                        geoAreaRepository = get<GeoAreaRepository>(),
                        geoNameEnrichmentRepository = get<GeoNameEnrichmentRepository>(),
                    )
                }
            },
        )
    }

    return app.koin
}
