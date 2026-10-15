package com.serkodesign.tepera.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.category.categoryIcon
import com.serkodesign.tepera.ui.category.categoryLineArtIconRes
import com.serkodesign.tepera.ui.theme.TeperaPalette
import com.serkodesign.tepera.ui.theme.TeperaSymbols
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect

/**
 * Глобальний міні-плеєр сесії (docs/HANDOFF.md, розділ 7; Figma `MiniPlayer` 679:12765): над навбаром на всіх
 * вкладках, крім Home, поки йде таймер. На Home цю роль виконує активний `ActivityRow`, тож дубля нема.
 *
 * - Стану **Paused** з Figma в Tepera немає: паузи таймера не існує — кнопка "стоп" зупиняє таймер і зберігає запис
 *   (та сама `toggleCategoryTimer`, що play/pause на Home, віджеті й у сповіщенні).
 * - Статус — "Триває" без часу: за раніше прийнятим рішенням власника тривалість сеансу не показується в UI.
 * - Тап по панелі веде до історії цієї категорії ([onOpen]).
 * - Без тіні (тіней у застосунку нема); матове скло як у навбара, під ним підкладка `bg/gradient-top` @90%.
 */
@Composable
fun MiniPlayer(
    category: CategoryEntity,
    onOpen: () -> Unit,
    onStop: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)
    val name = categoryDisplayName(category)
    val accent = categoryColor(category.colorHex)
    val underlay = (TeperaPalette.colors.backgroundTop ?: Color.White).copy(alpha = 0.9f)
    val border = TeperaPalette.activityCardIdleBorder

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp) // росте разом зі шрифтом, а не обрізає назву
            .clip(shape)
            .hazeEffect(state = hazeState, style = HazeStyle(tint = null, blurRadius = 12.dp))
            .background(underlay)
            .background(TeperaPalette.activityCardIdle)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            val lineArt = categoryLineArtIconRes(category.iconName)
            if (lineArt != null) {
                Icon(painterResource(lineArt), contentDescription = null, tint = TeperaPalette.textPrimary, modifier = Modifier.size(20.dp))
            } else {
                Icon(categoryIcon(category.iconName), contentDescription = null, tint = TeperaPalette.textPrimary, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = TeperaPalette.textPrimary,
                fontFamily = TeperaPalette.headlineFont,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 15.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(TeperaPalette.restOfDayCard))
                Text(
                    text = stringResource(R.string.activity_row_running),
                    color = TeperaPalette.textSecondary,
                    fontFamily = TeperaPalette.headlineFont,
                    fontSize = 12.sp, // ≥ 12sp для змістовного тексту (CH-10); у Figma 11
                    lineHeight = 13.2.sp,
                    maxLines = 1
                )
            }
        }
        // 40dp за макетом, але область дотику — не менше 48dp.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onStop),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(TeperaPalette.activityCardActive),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TeperaSymbols.Pause,
                    contentDescription = stringResource(R.string.category_stop_action) + ": " + name,
                    tint = TeperaPalette.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
