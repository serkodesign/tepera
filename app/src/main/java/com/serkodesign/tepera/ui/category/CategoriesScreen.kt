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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    // T-8 (tepera-dev-spec.md): ліміт кастомних категорій — 2, не 1 — рядок "Додати" лишається,
    // доки не зайняті обидва слоти (архівовані кастомні категорії й далі займають слот, FR-2.3).
    val customCategorySlotAvailable = categories.count { it.isCustom } < CategoryViewModel.MAX_CUSTOM_CATEGORIES

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.categories_screen_title), onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { GlassSectionHeader(stringResource(R.string.categories_section_active)) }
                items(active, key = { it.id }) { category ->
                    CategoryRow(
                        category = category,
                        isArchived = false,
                        onToggleArchive = { viewModel.archive(category.id) },
                        onDelete = { pendingDelete = category }
                    )
                }
                if (customCategorySlotAvailable) {
                    item {
                        GlassRow(
                            label = stringResource(R.string.category_add_custom),
                            onClick = { showCreateDialog = true },
                            leading = { TeperaIconCircle(TeperaSymbols.Add) },
                            trailing = { }
                        )
                    }
                }
                if (archived.isNotEmpty()) {
                    item { GlassSectionHeader(stringResource(R.string.categories_section_archived)) }
                    items(archived, key = { it.id }) { category ->
                        CategoryRow(
                            category = category,
                            isArchived = true,
                            onToggleArchive = { viewModel.unarchive(category.id) },
                            onDelete = { pendingDelete = category }
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
    onDelete: () -> Unit
) {
    GlassRow(
        label = categoryDisplayName(category),
        leading = {
            TeperaIconCircle(
                icon = categoryIcon(category.iconName),
                background = categoryColor(category.colorHex).copy(alpha = 0.2f),
                tint = categoryGlyphColor(category.colorHex)
            )
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // За прямим запитом користувача: справжнє видалення — лише для кастомних
                // категорій (звільняє слот ліміту T-8), дефолтні лишаються архів/розархівувати-
                // only (пересіваються щозапуску за фіксованим id, "видалення" воскресло б).
                if (category.isCustom) {
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
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(customCategoryIconChoices.first()) }
    var selectedColor by remember { mutableStateOf(customCategoryColorChoices.first()) }
    var showNameError by remember { mutableStateOf(false) }
    val nameRequiredMessage = stringResource(R.string.category_name_required)

    TeperaDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.category_dialog_title),
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; showNameError = false },
                label = { Text(stringResource(R.string.category_name_label)) },
                isError = showNameError,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    errorContainerColor = Color.White,
                    focusedBorderColor = TeperaPalette.buttonBrand,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = TeperaPalette.buttonBrand,
                    unfocusedLabelColor = TeperaPalette.buttonBrandDark.copy(alpha = 0.7f),
                    focusedTextColor = TeperaPalette.buttonBrandDark,
                    unfocusedTextColor = TeperaPalette.buttonBrandDark,
                    cursorColor = TeperaPalette.buttonBrand
                ),
                modifier = Modifier.fillMaxWidth().semantics { if (showNameError) error(nameRequiredMessage) }
            )
            if (showNameError) {
                Text(
                    nameRequiredMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp).semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.category_icon_label),
                style = MaterialTheme.typography.labelLarge,
                color = TeperaPalette.buttonBrandDark
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                customCategoryIconChoices.forEach { iconKey ->
                    SwatchPickable(
                        selected = selectedIcon == iconKey,
                        onClick = { selectedIcon = iconKey }
                    ) { tint ->
                        Icon(categoryIcon(iconKey), contentDescription = null, tint = tint)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.category_color_label),
                style = MaterialTheme.typography.labelLarge,
                color = TeperaPalette.buttonBrandDark
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                customCategoryColorChoices.forEach { colorHex ->
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
    }
}

@Composable
private fun SwatchPickable(selected: Boolean, onClick: () -> Unit, content: @Composable (tint: Color) -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(if (selected) TeperaPalette.buttonBrand else Color.White, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content(if (selected) Color.White else TeperaPalette.buttonBrandDark) }
}
