// Кореневий build-файл проєкту.
// ПРИМІТКА: версії плагінів нижче орієнтовні станом на момент написання (вересень 2026).
// Перед першою збіркою перевір актуальні версії в Android Studio (File → Project Structure)
// або на https://developer.android.com/studio/releases — вони змінюються часто.

plugins {
    id("com.android.application") version "9.2.1" apply false
    // Kotlin навмисно 2.3.21, не 2.4.x: остання опублікована KSP (2.3.11) ще не підтримує
    // Kotlin 2.4.x — при розсинхроні падає з "unexpected jvm signature V" (станом на 09.2026).
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
    // Crashlytics (Фаза 5) — окремий Gradle-плагін від google-services, потрібен для завантаження
    // mapping-файлів і символів крашів.
    id("com.google.firebase.crashlytics") version "3.0.8" apply false
}
