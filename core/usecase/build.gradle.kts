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
        }
    }
}
