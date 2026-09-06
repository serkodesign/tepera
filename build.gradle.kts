// Кореневий build-файл проєкту.
// ПРИМІТКА: версії плагінів нижче орієнтовні станом на момент написання (вересень 2026).
// Перед першою збіркою перевір актуальні версії в Android Studio (File → Project Structure)
// або на https://developer.android.com/studio/releases — вони змінюються часто.

plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false // обов'язковий окремий плагін з Kotlin 2.0+
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false // для Room
    id("com.google.gms.google-services") version "4.4.2" apply false // Firebase — підключити у фазі 4
}
