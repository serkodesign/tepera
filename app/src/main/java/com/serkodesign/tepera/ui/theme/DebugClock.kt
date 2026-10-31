package com.serkodesign.tepera.ui.theme

import androidx.compose.runtime.mutableStateOf

/**
 * ТИМЧАСОВИЙ налагоджувальний годинник теми: дозволяє «прогорнути» день і побачити тему «За часом» вдень, ввечері й
 * уночі без зміни годинника телефона. Лише в debug-збірках (перемикач у Налаштуваннях видно при `BuildConfig.DEBUG`),
 * у пам'яті — після перезапуску застосунку скидається. null = справжній час.
 */
object DebugClock {
    /** Хвилина доби 0..1439 або null. Читання в композиції перемальовує її при зміні. */
    val override = mutableStateOf<Int?>(null)
}
