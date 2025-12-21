package primitive

import com.android.build.api.dsl.androidLibrary
import me.matsumo.travelog.libs
import me.matsumo.travelog.version
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class KmpAndroidPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.kotlin.multiplatform.library")
                apply("kotlin-parcelize")
                apply("kotlinx-serialization")
                apply("project-report")
                apply("com.google.devtools.ksp")
            }

            kotlin {
                compilerOptions {
                    jvmToolchain(17)
                }

                androidLibrary {
                    compileSdk = libs.version("compileSdk").toInt()
                    androidResources.enable = true
                }
            }
        }
    }
}
