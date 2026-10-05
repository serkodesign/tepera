package com.serkodesign.tepera.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Текстове поле за Figma App concept 372:493 (Default) і 372:497 (Input): блок 8dp з усіх боків,
 * радіус 8, заливка суцільна #F0F3F4; підпис 11sp зверху і значення 16sp під ним з проміжком 2dp.
 * Рамка 1dp: у стані default — #F0F3F4, у фокусі — #006944.
 */
@Composable
fun TeperaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (focused) TeperaPalette.buttonBrand else TeperaPalette.inputSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(TeperaPalette.inputSurface, shape)
            .border(1.dp, borderColor, shape)
            // Горизонтальний відступ збільшено на 50% (8 → 12dp), висота блоку 56dp.
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            color = if (focused) TeperaPalette.buttonBrand else TeperaPalette.textSecondary
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            enabled = enabled,
            interactionSource = interactionSource,
            textStyle = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, color = Color(0xFF0F0F10)),
            cursorBrush = SolidColor(TeperaPalette.buttonBrand),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
