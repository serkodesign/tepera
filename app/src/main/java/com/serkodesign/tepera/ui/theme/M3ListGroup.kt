package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Списки Налаштувань і Головної v2 — Figma "Tepera" (k6s4prQ9oK9x2uUvzHRghR): T2 · Налаштування (1066:4191) і
 * 07 · Головна v2 (1081:2166). Підпис групи (`group label`), скляна картка групи з розділювачами й рядок `v32/ListItem`.
 * Кольори — токени M3 ([MaterialTheme.colorScheme], [M3Extra]), тож темна тема й настрої працюють без окремих значень.
 */

private val GroupShape = RoundedCornerShape(16.dp)

/** `group label`: Title Small кольору primary, відступ зліва 16dp, зверху 12dp, знизу 4dp. */
@Composable
fun M3GroupLabel(text: String, modifier: Modifier = Modifier, top: Dp = 12.dp, bottom: Dp = 4.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, top = top, bottom = bottom).semantics { heading() }
    )
}

/**
 * Скляна картка групи: `glass` 62%, обвідка 1dp `cardStroke`, радіус 16dp, поле 4dp зверху й знизу. Рядки — [M3ListItem],
 * між ними [M3ListDivider].
 */
@Composable
fun M3ListGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val extra = M3Extra.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(GroupShape)
            .background(extra.glass)
            .border(1.dp, extra.cardStroke, GroupShape)
            .padding(vertical = 4.dp),
        content = content
    )
}

/** Розділювач рядків групи: 1dp `outlineVariant` на всю ширину. */
@Composable
fun M3ListDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

/** Кружок іконки рядка: 40dp `surfaceContainerHighest`, гліф 20dp кольору onSurfaceVariant. */
@Composable
fun M3ListIcon(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

/**
 * `v32/ListItem`: від 56dp заввишки, поля 16×8dp, проміжок 12dp. Назва — Body Large onSurface, підпис — Body Medium
 * onSurfaceVariant, значення праворуч — Body Medium onSurfaceVariant. [trailing] за замовчуванням — шеврон, якщо рядок
 * клікабельний; для перемикача передати власний слот.
 */
@Composable
fun M3ListItem(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    supporting: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = if (onClick != null) ({ M3Chevron() }) else null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) M3ListIcon(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        trailing?.invoke()
    }
}

/** Шеврон рядка: 24dp, onSurfaceVariant. */
@Composable
fun M3Chevron(modifier: Modifier = Modifier) {
    Icon(
        TeperaSymbols.ChevronRight, contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier.size(24.dp)
    )
}
