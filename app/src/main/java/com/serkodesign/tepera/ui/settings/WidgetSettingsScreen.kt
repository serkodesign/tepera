package com.serkodesign.tepera.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.GlassSectionHeader
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val MAX_WIDGET_BUTTONS = 5

// Висота рядка GlassRow (padding 12 + кружок 40 + padding 12) + проміжок між рядками 8 —
// крок, на який зсувається сусід при перетягуванні.
private val ROW_STEP = 72.dp

/**
 * Налаштування кнопок віджета (за прямим запитом користувача): які категорії й у якому порядку
 * стоять на віджеті. Один спільний список для всіх розмірів — 1x1 бере першу, 2x1 — перші дві,
 * 4x1/4x3 — до п'яти. Порожній список = автоматичний порядок за порою доби (FR-4.5).
 * Порядок міняється перетягуванням за ручку ліворуч; зміни одразу зберігаються й віджети
 * оновлюються.
 */
@Composable
fun WidgetSettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as TeperaApp
    val scope = rememberCoroutineScope()

    val savedIds by app.settingsStore.widgetCategoryIds.collectAsState(initial = emptyList())
    val activeCategories by app.categoryRepository.observeActiveCategories().collectAsState(initial = emptyList())
    val byId = activeCategories.associateBy { it.id }

    // Локальна копія порядку — перетягування міняє її миттєво, у сховище пишемо лише в кінці жесту.
    var order by remember { mutableStateOf<List<String>>(emptyList()) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    // Архівовані/видалені категорії мовчки випадають з обраного (як і на самому віджеті).
    LaunchedEffect(savedIds, activeCategories) {
        if (draggingId == null) order = savedIds.filter { it in byId }
    }

    fun persist(ids: List<String>) {
        order = ids
        scope.launch {
            app.settingsStore.setWidgetCategoryIds(ids)
            app.activeTimerStore.refreshWidgets()
        }
    }

    val stepPx = with(LocalDensity.current) { ROW_STEP.toPx() }
    val available = activeCategories.filter { it.id !in order }.sortedBy { it.sortOrder }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.widget_settings_title), onBack = onBack)

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(
                        if (order.isEmpty()) R.string.widget_settings_auto_hint else R.string.widget_settings_hint
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                if (order.isNotEmpty()) {
                    GlassSectionHeader(stringResource(R.string.widget_settings_selected_section))
                    order.forEach { id ->
                        val category = byId[id] ?: return@forEach
                        key(id) {
                            val isDragging = draggingId == id
                            GlassRow(
                                label = categoryDisplayName(category),
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer { translationY = if (isDragging) dragOffset else 0f },
                                leading = {
                                    // Ручка перетягування — ліворуч, перед іконкою категорії.
                                    Icon(
                                        imageVector = Icons.Filled.DragHandle,
                                        contentDescription = stringResource(R.string.widget_settings_drag_handle),
                                        modifier = Modifier.pointerInput(id) {
                                            detectDragGestures(
                                                onDragStart = { draggingId = id; dragOffset = 0f },
                                                onDragEnd = {
                                                    draggingId = null; dragOffset = 0f
                                                    persist(order)
                                                },
                                                onDragCancel = {
                                                    draggingId = null; dragOffset = 0f
                                                    order = savedIds.filter { it in byId }
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    dragOffset += amount.y
                                                    val current = order.indexOf(id)
                                                    // Зсув на пів кроку — міняємось місцями з сусідом і віднімаємо
                                                    // пройдений крок, щоб рядок лишався під пальцем.
                                                    if (dragOffset > stepPx / 2 && current < order.lastIndex) {
                                                        order = order.moved(current, current + 1)
                                                        dragOffset -= stepPx
                                                    } else if (dragOffset < -stepPx / 2 && current > 0) {
                                                        order = order.moved(current, current - 1)
                                                        dragOffset += stepPx
                                                    }
                                                }
                                            )
                                        }
                                    )
                                    CategoryCircle(category)
                                },
                                trailing = {
                                    TeperaIconButton(
                                        icon = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.widget_settings_remove),
                                        onClick = { persist(order - id) }
                                    )
                                }
                            )
                        }
                    }
                }

                if (available.isNotEmpty()) {
                    GlassSectionHeader(stringResource(R.string.widget_settings_available_section))
                    val full = order.size >= MAX_WIDGET_BUTTONS
                    if (full) {
                        Text(
                            text = stringResource(R.string.widget_settings_limit, MAX_WIDGET_BUTTONS),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    available.forEach { category ->
                        GlassRow(
                            label = categoryDisplayName(category),
                            onClick = if (full) null else ({ persist(order + category.id) }),
                            leading = { CategoryCircle(category) },
                            trailing = {
                                if (!full) Icon(Icons.Filled.Add, contentDescription = null)
                            }
                        )
                    }
                }

                if (order.isNotEmpty()) {
                    TeperaButton(
                        text = stringResource(R.string.widget_settings_reset),
                        onClick = { persist(emptyList()) },
                        type = TeperaButtonType.Tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCircle(category: CategoryEntity) {
    val color = categoryColor(category.colorHex)
    TeperaIconCircle(
        icon = categoryIcon(category.iconName),
        background = color.copy(alpha = 0.2f),
        tint = color
    )
}

private fun List<String>.moved(from: Int, to: Int): List<String> =
    toMutableList().also { it.add(to, it.removeAt(from)) }
