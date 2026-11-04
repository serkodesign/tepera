package com.serkodesign.tepera.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Дизайн-система v3.1: ролі Tepera, яких нема в M3 (розділ 2.2 плану). Окремо від `ColorScheme`.
 * Читати через [M3Extra.colors]. Назви свідомо відмінні від наявних `TeperaColors`/`LocalTeperaColors`
 * (старий редизайн) — вони живуть паралельно до завершення міграції.
 */
@Immutable
data class TeperaExtraColors(
    val signal: Color,
    val onSignal: Color,
    val signalContainer: Color,
    val onSignalContainer: Color,
    val signalVivid: Color,
    val inverseSignal: Color,
    val card: Color,
    val cardStroke: Color,
    val onDark: Color,
    val glass: Color,
    val glassStrong: Color,
    val bgGradientTop: Color,
    val bgGradientBottom: Color,
    val bgBlobWarm: Color,
    val bgBlobCool: Color,
    val dataHeat0: Color,
    val dataHeat1: Color,
    val dataHeat2: Color,
    val dataHeat3: Color,
    val dataHeat4: Color,
    val dataHeat5: Color,
    val dataTrack: Color,
    val dataNature: Color,
    val dataReading: Color,
    val dataHobby: Color,
    val dataMovement: Color,
    val dataSocial: Color,
    val dataErrands: Color,
    val dataOnline: Color,
) {
    /** Клітинка хітмапи за рівнем 0–5 (значення поза діапазоном обмежуються). */
    fun heat(level: Int): Color = when (level.coerceIn(0, 5)) {
        0 -> dataHeat0
        1 -> dataHeat1
        2 -> dataHeat2
        3 -> dataHeat3
        4 -> dataHeat4
        else -> dataHeat5
    }
}

val LightTeperaExtraColors = M3Palette.ExtraLight.let { c ->
    TeperaExtraColors(
        signal = c.signal,
        onSignal = c.onSignal,
        signalContainer = c.signalContainer,
        onSignalContainer = c.onSignalContainer,
        signalVivid = c.signalVivid,
        inverseSignal = c.inverseSignal,
        card = c.card,
        cardStroke = c.cardStroke,
        onDark = c.onDark,
        glass = c.glass,
        glassStrong = c.glassStrong,
        bgGradientTop = c.bgGradientTop,
        bgGradientBottom = c.bgGradientBottom,
        bgBlobWarm = c.bgBlobWarm,
        bgBlobCool = c.bgBlobCool,
        dataHeat0 = c.dataHeat0,
        dataHeat1 = c.dataHeat1,
        dataHeat2 = c.dataHeat2,
        dataHeat3 = c.dataHeat3,
        dataHeat4 = c.dataHeat4,
        dataHeat5 = c.dataHeat5,
        dataTrack = c.dataTrack,
        dataNature = c.dataNature,
        dataReading = c.dataReading,
        dataHobby = c.dataHobby,
        dataMovement = c.dataMovement,
        dataSocial = c.dataSocial,
        dataErrands = c.dataErrands,
        dataOnline = c.dataOnline,
    )
}

val DarkTeperaExtraColors = M3Palette.ExtraDark.let { c ->
    TeperaExtraColors(
        signal = c.signal,
        onSignal = c.onSignal,
        signalContainer = c.signalContainer,
        onSignalContainer = c.onSignalContainer,
        signalVivid = c.signalVivid,
        inverseSignal = c.inverseSignal,
        card = c.card,
        cardStroke = c.cardStroke,
        onDark = c.onDark,
        glass = c.glass,
        glassStrong = c.glassStrong,
        bgGradientTop = c.bgGradientTop,
        bgGradientBottom = c.bgGradientBottom,
        bgBlobWarm = c.bgBlobWarm,
        bgBlobCool = c.bgBlobCool,
        dataHeat0 = c.dataHeat0,
        dataHeat1 = c.dataHeat1,
        dataHeat2 = c.dataHeat2,
        dataHeat3 = c.dataHeat3,
        dataHeat4 = c.dataHeat4,
        dataHeat5 = c.dataHeat5,
        dataTrack = c.dataTrack,
        dataNature = c.dataNature,
        dataReading = c.dataReading,
        dataHobby = c.dataHobby,
        dataMovement = c.dataMovement,
        dataSocial = c.dataSocial,
        dataErrands = c.dataErrands,
        dataOnline = c.dataOnline,
    )
}

/** Дефолт — світлий набір, щоб `@Preview` і неоновлені гілки не падали без провайдера. */
val LocalTeperaExtraColors = staticCompositionLocalOf { LightTeperaExtraColors }

/** Доступ до ролей Tepera: `M3Extra.colors.signal`. (У плані це `TeperaTheme.colors`.) */
object M3Extra {
    val colors: TeperaExtraColors
        @Composable get() = LocalTeperaExtraColors.current
}
