plugins {
    id("matsumo.primitive.kmp.common")
    id("matsumo.primitive.kmp.android")
    id("matsumo.primitive.kmp.ios")
    id("matsumo.primitive.kmp.jvm")
    id("matsumo.primitive.detekt")
}

kotlin {
    android {
        namespace = "me.matsumo.travelog.core.datasource"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.proto)
        }

        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:model"))
            implementation(project(":core:resource"))

            api(project.dependencies.platform(libs.supabase.bom))
            api(libs.bundles.supabase)
            api(libs.bundles.ktor)
            api(libs.bundles.filekit)
            api(libs.androidx.datastore.preferences)

            implementation(libs.kotlinx.datetime)
        }
    }
}
