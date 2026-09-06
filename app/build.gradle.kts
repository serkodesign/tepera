// ПРИМІТКА: версії бібліотек нижче орієнтовні — перевір актуальні перед збіркою.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    // id("com.google.gms.google-services") // розкоментувати у фазі 5 (Crashlytics)
}

android {
    namespace = "com.serkodesign.tepera"
    // compileSdk 37, окремо від targetSdk: новіший Compose BOM вимагає компіляції проти API 37,
    // але targetSdk (нижче) свідомо лишається 36 — саме targetSdk, а не compileSdk, регулює
    // Google Play політику з 31.08.2026 (SRS PUB-4, CLAUDE.md).
    compileSdk = 37

    defaultConfig {
        applicationId = "com.serkodesign.tepera"
        minSdk = 26        // Android 8.0 — нижня межа сумісності (SRS 5.6)
        targetSdk = 36      // Android 16 — обов'язково для Google Play з 31.08.2026 (SRS PUB-4)
        versionCode = 1
        versionName = "0.1.0-mvp"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

// NFR-5.3: явна стратегія Room-міграцій починається з експорту схеми — без цього немає з чим
// звіряти Migration-об'єкти, коли з'явиться перша зміна схеми.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.10.0")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // DataStore — анонімний device-ID (NFR-7.3) і налаштування таргету Online-часу (FR-3.4)
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    // v2.0: Health Connect ВИДАЛЕНО (перенесено на post-MVP, SRS 3.1)

    // Jetpack Glance — адаптивний домашній віджет (FR-4.1)
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.glance:glance-material3:1.2.0")

    // WorkManager — періодичне оновлення віджета (FR-4.3), ~30 хв інтервал
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    // Графіки — Vico, без власного chart-движка (out-of-scope, SRS розділ 6)
    implementation("com.patrykandpatrick.vico:compose-m3:3.3.1")

    // Crashlytics — підключити у фазі 5
    // implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    // implementation("com.google.firebase:firebase-crashlytics-ktx")
}
