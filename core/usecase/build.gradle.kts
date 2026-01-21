plugins {
    id("matsumo.primitive.kmp.common")
    id("matsumo.primitive.kmp.android")
    id("matsumo.primitive.kmp.ios")
    id("matsumo.primitive.detekt")
}

kotlin {
    android {
        namespace = "me.matsumo.travelog.core.usecase"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:repository"))
            implementation(project(":core:datasource"))
            implementation(libs.bundles.filekit)
            implementation(libs.koin.core)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core)
        }
    }
}
