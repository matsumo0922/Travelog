import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.server.application.Application
import io.ktor.server.application.install
import me.matsumo.travelog.core.common.di.commonModule
import me.matsumo.travelog.core.datasource.di.dataSourceModule
import me.matsumo.travelog.core.repository.di.repositoryModule
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.initKoin() {
    val supabaseClient = createSupabaseClient(
        supabaseUrl = System.getenv("SUPABASE_URL"),
        supabaseKey = System.getenv("SUPABASE_KEY"),
    ) {
        defaultSerializer = KotlinXSerializer(formatter)
        install(Postgrest)
    }

    install(Koin) {
        slf4jLogger()

        modules(
            module {
                single<SupabaseClient> { supabaseClient }
            },
            commonModule,
            dataSourceModule,
            repositoryModule,
        )
    }
}
