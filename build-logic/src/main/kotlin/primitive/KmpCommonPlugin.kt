package primitive

import me.matsumo.travelog.library
import me.matsumo.travelog.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpCommonPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
            }

            kotlin {
                applyDefaultHierarchyTemplate()

                compilerOptions {
                    freeCompilerArgs.add("-Xexplicit-backing-fields")
                }

                sourceSets.commonMain.dependencies {
                    val kotlinBom = libs.library("kotlin-bom")
                    implementation(project.dependencies.platform(kotlinBom))
                }
            }
        }
    }
}

fun Project.kotlin(action: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure(action)
}

fun Project.kotlinAndroid(action: KotlinAndroidProjectExtension.() -> Unit) {
    extensions.configure(action)
}
