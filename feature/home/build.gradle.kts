plugins {
    id("matsumo.primitive.kmp.common")
    id("matsumo.primitive.kmp.compose")
    id("matsumo.primitive.kmp.android")
    id("matsumo.primitive.kmp.ios")
    id("matsumo.primitive.detekt")
}

kotlin {
    android {
        namespace = "me.matsumo.travelog.feature.home"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:model"))
            implementation(project(":core:repository"))
            implementation(project(":core:datasource"))
            implementation(project(":core:usecase"))
            implementation(project(":core:ui"))
            implementation(project(":core:resource"))
        }
    }
}
