package com.serkodesign.tepera.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import android.content.Context
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.entity.CategoryEntity

/**
 * FR-2.1/FR-2.2: спільний каталог іконок для 6 дефолтних категорій (T-8: "Справи" додано) і для вибору іконки
 * кастомної категорії — той самий набір, той самий iconName -> ImageVector резолвер.
 */
private val iconCatalog: Map<String, ImageVector> = mapOf(
    "nature" to TeperaSymbols.Park,
    "reading" to TeperaSymbols.MenuBook,
    "hobby" to TeperaSymbols.Palette,
    "movement" to TeperaSymbols.DirectionsRun,
    "social" to TeperaSymbols.Groups,
    "errands" to TeperaSymbols.Checklist, // T-8 (tepera-dev-spec.md): нейтральна 6-та дефолтна категорія
    "sleep" to TeperaSymbols.Bedtime, // legacy, лише для вже заархівованих записів (v2.4)
    "star" to TeperaSymbols.Star,
    "favorite" to TeperaSymbols.Favorite,
    "coffee" to TeperaSymbols.Coffee,
    "music" to TeperaSymbols.MusicNote,
    "brush" to TeperaSymbols.Brush,
    "pets" to TeperaSymbols.Pets
)

/** Іконки, доступні користувачу при створенні кастомної категорії (FR-2.2, "іконка з набору"). */
val customCategoryIconChoices: List<String> =
    listOf("star", "favorite", "coffee", "music", "brush", "pets")

/** ЗАГЛУШКА: приглушена палітра до Фази 6 (Figma) — без яскравих "гейміфікованих" кольорів. */
val customCategoryColorChoices: List<String> = listOf(
    "#4E7A51", "#4A6FA5", "#B08968", "#C9704F", "#5C6B73", "#7A5C7A", "#8A8F5C"
)

fun categoryIcon(iconName: String): ImageVector = iconCatalog[iconName] ?: TeperaSymbols.Star

/**
 * Контурні "widget"-іконки (Figma "App concept" k6s4prQ9oK9x2uUvzHRghR, той самий набір book_5/
 * directions_bike/content_cut/footprint/partly_cloudy_night/camping/groups/checklist, що вже
 * імпортований для домашнього віджета) — тепер повторно використаний для сітки "Категорія" на
 * новому екрані додавання активності (node 61:3516), щоб відповідати макету пікселя в піксель.
 * Кастомні категорії (star/favorite/coffee/music/brush/pets) не мають цього стилю — null,
 * викликач падає назад на [categoryIcon] (Material-іконки, як і скрізь у застосунку).
 */
fun categoryLineArtIconRes(iconName: String): Int? = when (iconName) {
    "nature" -> com.serkodesign.tepera.R.drawable.ic_widget_nature
    "reading" -> com.serkodesign.tepera.R.drawable.ic_widget_reading
    "hobby" -> com.serkodesign.tepera.R.drawable.ic_widget_hobby
    "movement" -> com.serkodesign.tepera.R.drawable.ic_widget_movement
    "social" -> com.serkodesign.tepera.R.drawable.ic_widget_social
    "errands" -> com.serkodesign.tepera.R.drawable.ic_widget_errands
    "sleep" -> com.serkodesign.tepera.R.drawable.ic_widget_sleep
    else -> null
}

fun categoryColor(colorHex: String): Color = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
    .getOrDefault(Color.Gray)

/** WCAG 2.x 1.4.11 (Non-text Contrast): графічні об'єкти, зокрема іконки, — не менше 3:1 до фону. */
const val MIN_GLYPH_CONTRAST = 3f

/** Світла поверхня, на якій лежать плашки іконок (картка активності Home #EBFAE6 — найтемніша з типових). */
private val GlyphSurface = Color(0xFFEBFAE6)

/** Коефіцієнт контрасту WCAG 2.x між двома непрозорими кольорами: (L1 + 0.05) / (L2 + 0.05). */
fun contrastRatio(a: Color, b: Color): Float {
    val l1 = a.luminance()
    val l2 = b.luminance()
    return (maxOf(l1, l2) + 0.05f) / (minOf(l1, l2) + 0.05f)
}

/**
 * Колір гліфа категорії на її плашці (сам колір категорії з прозорістю [badgeAlpha] поверх світлої
 * поверхні). Яскраві кольори (бірюзовий, рожевий, салатовий…) на власній 20%-й плашці мають
 * контраст ~1.5–2:1 — гліф майже зливається. Якщо контраст нижче [MIN_GLYPH_CONTRAST], колір
 * затемнюється до чорного кроками по 5%, доки не досягне порогу; тон лишається тим самим, тож
 * категорію все ще видно за кольором. Кольори, що вже проходять, лишаються без змін.
 */
fun categoryGlyphColor(base: Color, badgeAlpha: Float = 0.2f): Color {
    val badge = base.copy(alpha = badgeAlpha).compositeOver(GlyphSurface)
    var step = 0
    var color = base
    while (contrastRatio(color, badge) < MIN_GLYPH_CONTRAST && step < 20) {
        step++
        color = lerp(base, Color.Black, step * 0.05f)
    }
    return color
}

fun categoryGlyphColor(colorHex: String, badgeAlpha: Float = 0.2f): Color =
    categoryGlyphColor(categoryColor(colorHex), badgeAlpha)

/**
 * Локалізована назва категорії: для дефолтних резолвиться через nameKey у strings.xml
 * (UA/EN), для кастомної — буквальний текст, який ввів користувач.
 */
@Composable
fun categoryDisplayName(category: CategoryEntity): String =
    categoryDisplayName(category) { stringResource(it) }

/**
 * Non-Compose варіант для Jetpack Glance (FR-4.1) — Glance-композиції не мають доступу до
 * compose-ui's stringResource(), лише до звичайного Context.getString().
 */
fun categoryDisplayName(category: CategoryEntity, context: Context): String =
    categoryDisplayName(category) { context.getString(it) }

private inline fun categoryDisplayName(category: CategoryEntity, resolve: (Int) -> String): String =
    when (category.nameKey) {
        "nature" -> resolve(R.string.category_nature)
        "reading" -> resolve(R.string.category_reading)
        "hobby" -> resolve(R.string.category_hobby)
        "movement" -> resolve(R.string.category_movement)
        "social" -> resolve(R.string.category_social)
        "errands" -> resolve(R.string.category_errands)
        "sleep" -> resolve(R.string.category_sleep) // legacy, вже заархівовані записи (v2.4)
        else -> category.name
    }
