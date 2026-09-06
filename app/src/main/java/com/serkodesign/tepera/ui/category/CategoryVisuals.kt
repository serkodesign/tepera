package com.serkodesign.tepera.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import android.content.Context
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.entity.CategoryEntity

/**
 * FR-2.1/FR-2.2: спільний каталог іконок для 5 дефолтних категорій і для вибору іконки
 * кастомної категорії — той самий набір, той самий iconName -> ImageVector резолвер.
 */
private val iconCatalog: Map<String, ImageVector> = mapOf(
    "nature" to Icons.Outlined.Park,
    "reading" to Icons.AutoMirrored.Outlined.MenuBook,
    "hobby" to Icons.Outlined.Palette,
    "movement" to Icons.AutoMirrored.Outlined.DirectionsRun,
    "sleep" to Icons.Outlined.Bedtime,
    "star" to Icons.Outlined.Star,
    "favorite" to Icons.Outlined.Favorite,
    "coffee" to Icons.Outlined.Coffee,
    "music" to Icons.Outlined.MusicNote,
    "brush" to Icons.Outlined.Brush,
    "pets" to Icons.Outlined.Pets
)

/** Іконки, доступні користувачу при створенні кастомної категорії (FR-2.2, "іконка з набору"). */
val customCategoryIconChoices: List<String> =
    listOf("star", "favorite", "coffee", "music", "brush", "pets")

/** ЗАГЛУШКА: приглушена палітра до Фази 6 (Figma) — без яскравих "гейміфікованих" кольорів. */
val customCategoryColorChoices: List<String> = listOf(
    "#4E7A51", "#4A6FA5", "#B08968", "#C9704F", "#5C6B73", "#7A5C7A", "#8A8F5C"
)

fun categoryIcon(iconName: String): ImageVector = iconCatalog[iconName] ?: Icons.Outlined.Star

fun categoryColor(colorHex: String): Color = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
    .getOrDefault(Color.Gray)

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
        "sleep" -> resolve(R.string.category_sleep)
        else -> category.name
    }
