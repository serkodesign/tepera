package com.serkodesign.tepera.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.serkodesign.tepera.R
import com.serkodesign.tepera.util.roundToQuarterHour

/**
 * Округлення до 15 хв (за запитом користувача, замість "245 хв") — напр. "3 год 45 хв",
 * а не "3 год 47 хв". Використовується картками Home (напр. [WelcomeBackCard]); точні хвилини для hero —
 * `heroDuration` у HomeHero.kt.
 */
@Composable
internal fun formatBalanceDuration(minutes: Int): String {
    val (hours, remainderMinutes) = roundToQuarterHour(minutes)
    return when {
        hours <= 0 -> stringResource(R.string.minutes_short_format, remainderMinutes)
        remainderMinutes == 0 -> stringResource(R.string.hours_short_format, hours)
        else -> stringResource(R.string.hours_minutes_short_format, hours, remainderMinutes)
    }
}
