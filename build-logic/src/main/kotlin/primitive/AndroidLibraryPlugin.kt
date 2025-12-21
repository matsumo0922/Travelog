package primitive

import com.android.build.api.dsl.androidLibrary
import me.matsumo.travelog.libs
import me.matsumo.travelog.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.kotlin.multiplatform.library")
                apply("kotlin-parcelize")
                apply("kotlinx-serialization")
                apply("project-report")
                apply("com.google.devtools.ksp")
            }


            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                extensions.configure<KotlinMultiplatformExtension> {
                    androidLibrary {
                        compileSdk = libs.version("compileSdk").toInt()
                    }
                }
            }
        }
    }
}
