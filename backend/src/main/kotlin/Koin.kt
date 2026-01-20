import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.application.install
import me.matsumo.travelog.core.common.di.commonModule
import me.matsumo.travelog.core.datasource.GeminiDataSource
import me.matsumo.travelog.core.datasource.api.GeoAreaApi
import me.matsumo.travelog.core.datasource.di.dataSourceModule
import me.matsumo.travelog.core.repository.GeoAreaRepository
import me.matsumo.travelog.core.repository.GeoBoundaryRepository
import me.matsumo.travelog.core.repository.GeoNameEnrichmentRepository
import me.matsumo.travelog.core.repository.di.repositoryModule
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import service.BatchProcessingService

fun Application.initKoin() {
    val supabaseClient = createSupabaseClient(
        supabaseUrl = System.getenv("SUPABASE_URL"),
        supabaseKey = System.getenv("SUPABASE_KEY"),
    ) {
        defaultSerializer = KotlinXSerializer(formatter)
        install(Postgrest)
    }

    val geminiApiKey = System.getenv("GEMINI_API_KEY") ?: ""

    install(Koin) {
        slf4jLogger()

        modules(
            module {
                single<SupabaseClient> { supabaseClient }
            },
            commonModule,
            dataSourceModule,
            repositoryModule,
            // Backend-specific modules
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
}
