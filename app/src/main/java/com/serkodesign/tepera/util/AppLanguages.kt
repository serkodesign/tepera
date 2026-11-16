package com.serkodesign.tepera.util

/**
 * Мови інтерфейсу, які людина може обрати в Налаштуваннях. Назва — рідною мовою (не перекладається).
 *
 * Додати мову: 1) папка `res/values-<код>/strings.xml` (код локалі Android, напр. `pl`, `pt-rBR`),
 * 2) рядок сюди з BCP-47 тегом (`pl`, `pt-BR`). `locales_config.xml` для системних «Мов застосунку» генерується
 * з папок ресурсів автоматично (`androidResources.generateLocaleConfig`). Мова за замовчуванням (`res/values`) — англійська.
 */
data class AppLanguage(val tag: String, val endonym: String)

object AppLanguages {
    val all: List<AppLanguage> = listOf(
        AppLanguage("uk", "Українська"),
        AppLanguage("en", "English")
    )

    /** Рідна назва мови за тегом; для невідомого тегу — сам тег. `null` — коли тег порожній (системна). */
    fun endonym(tag: String): String? = if (tag.isEmpty()) null else all.firstOrNull { it.tag == tag }?.endonym ?: tag

    /** Короткий код для значення в рядку Налаштувань (макет 1.3.0): `uk` → «UA», інші — мова великими літерами. `null` — системна. */
    fun shortCode(tag: String): String? = when {
        tag.isEmpty() -> null
        tag == "uk" -> "UA"
        else -> tag.substringBefore('-').uppercase()
    }
}
