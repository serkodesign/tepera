package com.serkodesign.tepera.ui.category

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
    val hasCustomCategoryEver = categories.any { it.isCustom }

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
                        onToggleArchive = { viewModel.archive(category.id) }
                    )
                }
                if (!hasCustomCategoryEver) {
                    item {
                        GlassRow(
                            label = stringResource(R.string.category_add_custom),
                            onClick = { showCreateDialog = true },
                            leading = { TeperaIconCircle(Icons.Filled.Add) },
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
                            onToggleArchive = { viewModel.unarchive(category.id) }
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
        AlertDialog(
            onDismissRequest = { limitReachedNotice = false },
            confirmButton = {
                TextButton(onClick = { limitReachedNotice = false }) { Text(stringResource(R.string.dialog_ok)) }
            },
            text = { Text(stringResource(R.string.category_custom_limit_reached)) }
        )
    }
}

@Composable
private fun CategoryRow(category: CategoryEntity, isArchived: Boolean, onToggleArchive: () -> Unit) {
    GlassRow(
        label = categoryDisplayName(category),
        leading = {
            TeperaIconCircle(
                icon = categoryIcon(category.iconName),
                background = categoryColor(category.colorHex).copy(alpha = 0.2f),
                tint = categoryColor(category.colorHex)
            )
        },
        trailing = {
            Switch(
                checked = !isArchived,
                onCheckedChange = { onToggleArchive() },
                colors = teperaSwitchColors()
            )
        }
    )
}

@Composable
private fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(customCategoryIconChoices.first()) }
    var selectedColor by remember { mutableStateOf(customCategoryColorChoices.first()) }
    var showNameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showNameError = false },
                    label = { Text(stringResource(R.string.category_name_label)) },
                    isError = showNameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showNameError) {
                    Text(
                        stringResource(R.string.category_name_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    stringResource(R.string.category_icon_label),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    customCategoryIconChoices.forEach { iconKey ->
                        SwatchPickable(
                            selected = selectedIcon == iconKey,
                            onClick = { selectedIcon = iconKey }
                        ) {
                            Icon(categoryIcon(iconKey), contentDescription = null)
                        }
                    }
                }

                Text(
                    stringResource(R.string.category_color_label),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    customCategoryColorChoices.forEach { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(categoryColor(colorHex), CircleShape)
                                .then(
                                    if (selectedColor == colorHex) {
                                        Modifier.border(
                                            BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                                            CircleShape
                                        )
                                    } else Modifier
                                )
                                .clickable { selectedColor = colorHex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    showNameError = true
                } else {
                    onSave(name.trim(), selectedIcon, selectedColor)
                }
            }) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        }
    )
}

@Composable
private fun SwatchPickable(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}
