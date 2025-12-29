plugins {
    id("matsumo.primitive.kmp.common")
    id("matsumo.primitive.kmp.android")
    id("matsumo.primitive.kmp.ios")
    id("matsumo.primitive.kmp.jvm")
    id("matsumo.primitive.detekt")
}

kotlin {
    android {
        namespace = "me.matsumo.travelog.core.model"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:resource"))

            implementation(libs.ktor.core)
        }
    }
}
