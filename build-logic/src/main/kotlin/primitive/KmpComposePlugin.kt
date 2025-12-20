package primitive

import me.matsumo.travelog.android
import me.matsumo.travelog.androidTestImplementation
import me.matsumo.travelog.debugImplementation
import me.matsumo.travelog.implementation
import me.matsumo.travelog.library
import me.matsumo.travelog.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class KmpComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            android {
                buildFeatures.compose = true
            }

            dependencies {
                val bom = libs.library("compose-bom")

                implementation(project.dependencies.platform(bom))
                implementation(libs.library("compose-ui-tooling-preview"))
                debugImplementation(libs.library("compose-ui-tooling"))
                androidTestImplementation(project.dependencies.platform(bom))
            }
        }
    }
}
