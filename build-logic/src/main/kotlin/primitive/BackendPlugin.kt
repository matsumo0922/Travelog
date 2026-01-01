package primitive

import me.matsumo.travelog.bundle
import me.matsumo.travelog.implementation
import me.matsumo.travelog.library
import me.matsumo.travelog.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class BackendPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("org.jetbrains.kotlin.jvm")
                apply("io.ktor.plugin")
                apply("kotlinx-serialization")
            }

            extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions {
                    freeCompilerArgs.add("-Xexplicit-backing-fields")
                }
            }

            dependencies {
                implementation(libs.bundle("ktor-server"))
                implementation(platform(libs.library("supabase-bom")))
                implementation(libs.library("supabase-postgrest"))
            }
        }
    }
}
