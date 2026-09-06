// ПРИМІТКА: версії бібліотек нижче орієнтовні — перевір актуальні перед збіркою.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") // обов'язковий з Kotlin 2.0+, без нього compose { } нижче не спрацює
    id("com.google.devtools.ksp")
    // id("com.google.gms.google-services") // розкоментувати у фазі 5 (Crashlytics)
}

android {
    namespace = "com.serkodesign.tepera"
    compileSdk = 36

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
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore — анонімний device-ID (NFR-7.3) і налаштування таргету Online-часу (FR-3.4)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // v2.0: Health Connect ВИДАЛЕНО (перенесено на post-MVP, SRS 3.1)

    // Jetpack Glance — адаптивний домашній віджет (FR-4.1)
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // WorkManager — періодичне оновлення віджета (FR-4.3), ~30 хв інтервал
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Графіки — Vico, без власного chart-движка (out-of-scope, SRS розділ 6)
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-alpha.28")

    // Crashlytics — підключити у фазі 5
    // implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    // implementation("com.google.firebase:firebase-crashlytics-ktx")
}
