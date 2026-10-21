package com.serkodesign.tepera.ui.category

import com.serkodesign.tepera.ui.theme.TeperaSymbols
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.serkodesign.tepera.ui.theme.TeperaPalette

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.OutlinedTextFieldDefaults

import com.serkodesign.tepera.ui.theme.TeperaDialog
import com.serkodesign.tepera.ui.theme.TeperaTextField

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.ui.theme.TeperaIconButton
import com.serkodesign.tepera.ui.theme.TeperaButtonType
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.repository.CategoryRepository
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.GlassSectionHeader
import com.serkodesign.tepera.ui.theme.TeperaIconCircle
import com.serkodesign.tepera.ui.theme.TeperaIcons
import com.serkodesign.tepera.ui.theme.teperaSwitchColors

/**
 * Стиль перенесений з Figma-фрейму Everyday_Designs (node 1951:1556, "Settings - Categories"):
 * "скляні" рядки замість Card, перемикач замість кнопки архів/розархівувати (checked = активна,
 * unchecked = архівована — та сама архівація/розархівація, лише через Switch). У фреймі немає
 * "Додати свою категорію" (демо лише для 5 фіксованих), але це реальна вимога FR-2.2 — за
 * узгодженням з користувачем додано як рядок унизу секції "Активні", коли кастомного слоту
 * ще не зайнято.
 */
@Composable
fun CategoriesScreen(
    repository: CategoryRepository,
    onBack: () -> Unit
) {
    val viewModel: CategoryViewModel = viewModel(factory = CategoryViewModel.Factory(repository))
    val categories by viewModel.allCategories.collectAsState()
    val createResult by viewModel.createResult.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var limitReachedNotice by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var pendingEdit by remember { mutableStateOf<CategoryEntity?>(null) }

    LaunchedEffect(createResult) {
        when (createResult) {
            CreateCategoryResult.Success -> {
                showCreateDialog = false
                viewModel.consumeCreateResult()
            }
            CreateCategoryResult.LimitReached -> {
                limitReachedNotice = true
                viewModel.consumeCreateResult()
            }
            null -> Unit
        }
    }

    val active = categories.filter { !it.isHidden }.sortedBy { it.sortOrder }
    val archived = categories.filter { it.isHidden }.sortedBy { it.sortOrder }
    // Ліміт кастомних категорій (CategoryViewModel.MAX_CUSTOM_CATEGORIES, за прямим запитом
    // користувача піднято до 5) — рядок "Додати" лишається, доки не зайняті всі слоти
    // (архівовані кастомні категорії й далі займають слот, FR-2.3).
    val customCategorySlotAvailable = categories.count { it.isCustom } < CategoryViewModel.MAX_CUSTOM_CATEGORIES

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.categories_screen_title), onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { GlassSectionHeader(stringResource(R.string.categories_section_active)) }
                if (customCategorySlotAvailable) {
                    item {
                        GlassRow(
                            label = stringResource(R.string.category_add_custom),
                            cornerRadius = 24.dp,
                            onClick = { showCreateDialog = true },
                            leading = { TeperaIconCircle(TeperaSymbols.Add) },
                            trailing = { }
                        )
                    }
                }
                items(active, key = { it.id }) { category ->
                    CategoryRow(
                        category = category,
                        isArchived = false,
                        onToggleArchive = { viewModel.archive(category.id) },
                        onDelete = { pendingDelete = category },
                        onEdit = { pendingEdit = category }
                    )
                }
                if (archived.isNotEmpty()) {
                    item { GlassSectionHeader(stringResource(R.string.categories_section_archived)) }
                    items(archived, key = { it.id }) { category ->
                        CategoryRow(
                            category = category,
                            isArchived = true,
                            onToggleArchive = { viewModel.unarchive(category.id) },
                            onDelete = { pendingDelete = category },
                            onEdit = { pendingEdit = category }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCategoryDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { name, icon, color -> viewModel.createCustomCategory(name, icon, color) }
        )
    }

    pendingEdit?.let { category ->
        val originalName = categoryDisplayName(category)
        EditCategoryDialog(
            category = category,
            onDismiss = { pendingEdit = null },
            onSave = { name, icon, color ->
                viewModel.updateCategory(category.id, name, icon, color, nameChanged = name != originalName)
                pendingEdit = null
            }
        )
    }

    if (limitReachedNotice) {
        TeperaDialog(
            onDismissRequest = { limitReachedNotice = false },
            text = stringResource(R.string.category_custom_limit_reached),
            confirmText = stringResource(R.string.dialog_ok),
            onConfirm = { limitReachedNotice = false }
        )
    }

    pendingDelete?.let { category ->
        TeperaDialog(
            onDismissRequest = { pendingDelete = null },
            title = stringResource(R.string.category_delete_confirm_title),
            text = stringResource(R.string.category_delete_confirm_body, categoryDisplayName(category)),
            confirmText = stringResource(R.string.category_delete_action),
            onConfirm = {
                viewModel.deleteCustomCategory(category.id)
                pendingDelete = null
            },
            dismissText = stringResource(R.string.dialog_cancel)
        )
    }
}

@Composable
private fun CategoryRow(
    category: CategoryEntity,
    isArchived: Boolean,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    GlassRow(
        label = categoryDisplayName(category),
        cornerRadius = 24.dp,
        leading = {
            // За прямим запитом користувача: та сама іконка, що на картці категорії на Home
            // (контурний Figma-гліф, якщо є, інакше Material Symbols) — раніше тут завжди був
            // Material-гліф, навіть коли Home показувала контурну Figma-іконку для тієї ж категорії.
            CategoryIconBadge(iconName = category.iconName, colorHex = category.colorHex)
        },
        trailing = {
            // За запитом: проміжки між редагуванням, видаленням і тоглом — 4dp.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                // Редагування (назва/іконка/колір) — для всіх категорій, зокрема дефолтних (CategoryViewModel.updateCategory).
                TeperaIconButton(icon = TeperaIcons.Edit, contentDescription = stringResource(R.string.category_edit_action), onClick = onEdit)
                if (category.isCustom) {
                    // За прямим запитом користувача: справжнє видалення — лише для кастомних
                    // категорій (звільняє слот ліміту T-8), дефолтні лишаються архів/розархівувати-
                    // only (пересіваються щозапуску за фіксованим id, "видалення" воскресло б).
                    TeperaIconButton(icon = TeperaSymbols.Delete, contentDescription = stringResource(R.string.category_delete_action), onClick = onDelete)
                }
                Switch(
                    checked = !isArchived,
                    onCheckedChange = { onToggleArchive() },
                    colors = teperaSwitchColors()
                )
            }
        }
    )
}

@Composable
internal fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String, colorHex: String) -> Unit
) {
    CategoryEditorDialog(
        title = stringResource(R.string.category_dialog_title),
        onDismiss = onDismiss,
        onSave = onSave
    )
}

/**
 * Редагування кастомної категорії (за прямим запитом користувача) — той самий діалог, що
 * створення, попередньо заповнений наявними назвою/іконкою/кольором. Лише для `isCustom`
 * категорій (виклик з [CategoriesScreen] уже це гарантує) — дефолтні не мають поля назви для
 * редагування (nameKey -> strings.xml), а зміна кольору дефолтної категорії мовчки відкотилась
 * би на наступному запуску (`CategoryRepository.ensureDefaultsSeeded()`).
 */
@Composable
internal fun EditCategoryDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String, colorHex: String) -> Unit
) {
    CategoryEditorDialog(
        title = stringResource(R.string.category_edit_title),
        initialName = categoryDisplayName(category),
        initialIcon = category.iconName,
        initialColor = category.colorHex,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

/** Кількість плиток у ряду вибору іконки/кольору (36 іконок і 18 кольорів діляться на 6 без залишку). */
private const val SWATCH_COLUMNS = 6

/** Бічний відступ вмісту `TeperaDialog` — ряд кольорів виходить за нього до країв діалогу. */
private val DIALOG_CONTENT_PADDING = 24.dp

/** Висота прокручуваної сітки іконок у діалозі: 3 ряди плиток (~39dp) + проміжки + півряду. */
private val ICON_GRID_HEIGHT = 164.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    title: String,
    initialName: String = "",
    initialIcon: String = customCategoryIconChoices.first(),
    initialColor: String = customCategoryColorChoices.first(),
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var showNameError by remember { mutableStateOf(false) }
    val colorScroll = rememberScrollState()
    val iconScroll = rememberScrollState()
    val nameRequiredMessage = stringResource(R.string.category_name_required)

    TeperaDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = stringResource(R.string.dialog_save),
        onConfirm = {
            if (name.isBlank()) {
                showNameError = true
            } else {
                onSave(name.trim(), selectedIcon, selectedColor)
            }
        },
        dismissText = stringResource(R.string.dialog_cancel)
    ) {
        // Рядок "прев'ю + назва": плитка показує обрані іконку й колір наживо (так само, як рядок категорії у списку).
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryIconBadge(iconName = selectedIcon, colorHex = selectedColor, size = 56.dp, badgeAlpha = 0.2f)
                // Поле за Figma 372:493/372:497 (власний компонент, підпис усередині блоку).
                TeperaTextField(
                    value = name,
                    onValueChange = { name = it; showNameError = false },
                    label = stringResource(R.string.category_name_label),
                    modifier = Modifier.weight(1f).semantics { if (showNameError) error(nameRequiredMessage) }
                )
            }
            if (showNameError) {
                Text(
                    nameRequiredMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }

        // Поточний колір категорії, якого нема в палітрі (дефолтні мають власні), додається першим.
        val colorChoices = remember(initialColor) {
            if (initialColor in customCategoryColorChoices) customCategoryColorChoices else listOf(initialColor) + customCategoryColorChoices
        }
        // Колір — один рядок зі свайпом убік (18 кружків); обраний — з обвідкою.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.category_color_label),
                style = MaterialTheme.typography.labelLarge,
                color = TeperaPalette.buttonBrandDark
            )
            Row(
                modifier = Modifier
                    .bleedHorizontally(DIALOG_CONTENT_PADDING)
                    .fadingScrollEdges(colorScroll, vertical = false)
                    .horizontalScroll(colorScroll)
                    .padding(horizontal = DIALOG_CONTENT_PADDING),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colorChoices.forEach { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(4.dp)
                            .background(categoryColor(colorHex), CircleShape)
                            .then(
                                if (selectedColor == colorHex) {
                                    Modifier.border(BorderStroke(2.dp, TeperaPalette.buttonBrandDark), CircleShape)
                                } else Modifier
                            )
                            .selectable(
                                selected = selectedColor == colorHex,
                                role = Role.RadioButton,
                                onClick = { selectedColor = colorHex }
                            )
                            .semantics { contentDescription = colorHex }
                    )
                }
            }
        }

        // Іконка — сітка 6 у ряд фіксованої висоти (~3,5 ряди: напівпобачений рядок підказує, що можна гортати).
        // Поточна іконка категорії, якої нема в наборі (дефолтні "nature"/"reading"/…), додається першою.
        val iconChoices = remember(initialIcon) {
            if (initialIcon in customCategoryIconChoices) customCategoryIconChoices else listOf(initialIcon) + customCategoryIconChoices
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.category_icon_label),
                style = MaterialTheme.typography.labelLarge,
                color = TeperaPalette.buttonBrandDark
            )
            FlowRow(
                maxItemsInEachRow = SWATCH_COLUMNS,
                modifier = Modifier.height(ICON_GRID_HEIGHT).fadingScrollEdges(iconScroll, vertical = true).verticalScroll(iconScroll),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                iconChoices.forEach { iconKey ->
                    if (selectedIcon == iconKey) {
                        // Обрана іконка фарбується в обраний колір (фон, гліф і обвідка).
                        val accent = categoryColor(selectedColor)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(accent.copy(alpha = 0.2f), CircleShape)
                                .border(BorderStroke(2.dp, accent), CircleShape)
                                .selectable(selected = true, role = Role.RadioButton, onClick = {}),
                            contentAlignment = Alignment.Center
                        ) {
                            CategoryGridGlyph(iconKey, categoryGlyphColor(accent))
                        }
                    } else {
                        SwatchPickable(
                            selected = false,
                            onClick = { selectedIcon = iconKey },
                            modifier = Modifier.weight(1f)
                        ) { tint ->
                            CategoryGridGlyph(iconKey, tint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwatchPickable(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (tint: Color) -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(if (selected) TeperaPalette.buttonBrand else TeperaPalette.chipSurface, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content(if (selected) TeperaPalette.onPrimary else TeperaPalette.buttonBrandDark) }
}

/** Гліф у плитці вибору: контурна Figma-іконка (як на Home), якщо вона є для цього ключа, інакше Material Symbols. */
@Composable
private fun CategoryGridGlyph(iconKey: String, tint: Color) {
    val lineArt = categoryLineArtIconRes(iconKey)
    if (lineArt != null) {
        Icon(painterResource(lineArt), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    } else {
        Icon(categoryIcon(iconKey), contentDescription = null, tint = tint)
    }
}

/**
 * М'яке згасання (до прозорого) на тих краях прокручуваної області, за якими ще є вміст: контент не обрізається
 * різко, а розчиняється. Глибина згасання росте плавно від 0 на самому краю вмісту до [length] — тож на початку
 * списку верхній/лівий край чистий.
 */
private fun Modifier.fadingScrollEdges(state: ScrollState, vertical: Boolean, length: Dp = 24.dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fade = length.toPx()
            val total = if (vertical) size.height else size.width
            val startStrength = (state.value / fade).coerceIn(0f, 1f)
            val endStrength = ((state.maxValue - state.value) / fade).coerceIn(0f, 1f)
            fun mask(from: Float, to: Float, keepFrom: Float, keepTo: Float) {
                val colors = listOf(Color.Black.copy(alpha = keepFrom), Color.Black.copy(alpha = keepTo))
                val brush = if (vertical) Brush.verticalGradient(colors, startY = from, endY = to)
                else Brush.horizontalGradient(colors, startX = from, endX = to)
                val topLeft = if (vertical) Offset(0f, minOf(from, to)) else Offset(minOf(from, to), 0f)
                val rect = if (vertical) Size(size.width, fade) else Size(fade, size.height)
                drawRect(brush, topLeft, rect, blendMode = BlendMode.DstIn)
            }
            if (startStrength > 0f) mask(0f, fade, 1f - startStrength, 1f)
            if (endStrength > 0f) mask(total - fade, total, 1f, 1f - endStrength)
        }

/** Розтягує елемент на [extra] за обидва бічні краї батька (поверх його відступів), не змінюючи розмір, який бачить батько. */
private fun Modifier.bleedHorizontally(extra: Dp): Modifier = layout { measurable, constraints ->
    val extraPx = extra.roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = 2 * extraPx))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extraPx, 0) }
}
