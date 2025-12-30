import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.util.Properties

plugins {
    id("matsumo.primitive.backend")
    id("matsumo.primitive.detekt")
}

group = "me.matsumo.blog.backend"
version = "0.0.1"

application {
    val localProperties = Properties().apply {
        project.rootDir.resolve("local.properties").takeIf { it.exists() }?.also {
            load(project.rootDir.resolve("local.properties").inputStream())
        }
    }

    mainClass.set("io.ktor.server.netty.EngineMain")
    applicationDefaultJvmArgs = listOfNotNull(
        localProperties.getJvmArg("SUPABASE_URL"),
        localProperties.getJvmArg("SUPABASE_KEY"),
        localProperties.getJvmArg("PORT", defaultValue = "8080"),
        localProperties.getJvmArg("REVISION", defaultValue = "UNKNOWN"),
    )
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:datasource"))
    implementation(project(":core:repository"))

    implementation(project.dependencies.platform(libs.kotlin.bom))

    implementation(libs.bundles.infra)
    implementation(libs.bundles.koin.server)
    implementation(libs.bundles.ktor)
}

tasks {
    register("stage").dependsOn("installDist")
}

fun Properties.getJvmArg(
    key: String,
    propertyKey: String = key,
    defaultValue: String? = null,
): String? {
    val value = getProperty(propertyKey)
    val env = System.getenv(key)

    return (value ?: env ?: defaultValue)?.let { "-D$key=$it" }
}
