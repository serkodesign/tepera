package com.serkodesign.tepera.util

import com.serkodesign.tepera.data.local.entity.SleepWindowEntity
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

/**
 * CC-6: ротація текстів екрана паузи воріт «мішком» — кожен текст показується один раз, поки набір не
 * закінчиться; потім нове перемішування. Чиста логіка (без Android) — тестується без пристрою.
 */
object GateTexts {
    const val TOTAL = 27

    /** Нічні тексти (25–27 у списку, індекси 24..26) — лише вночі. */
    val NIGHT_INDICES: Set<Int> = setOf(24, 25, 26)

    const val DEFAULT_NIGHT_START_HOUR = 22
    const val DEFAULT_NIGHT_END_HOUR = 6

    /**
     * Чи «ніч» для нічних текстів: усередині ввімкненого вікна сну користувача, а якщо жодного вікна
     * не ввімкнено — з 22:00 до 06:00 за локальним часом.
     */
    fun isNight(
        nowMillis: Long,
        enabledSleepWindows: List<SleepWindowEntity>,
        zone: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        if (enabledSleepWindows.isNotEmpty()) return SleepWindowCalculator.isInsideWindow(enabledSleepWindows, nowMillis)
        val hour = Instant.ofEpochMilli(nowMillis).atZone(zone).hour
        return hour >= DEFAULT_NIGHT_START_HOUR || hour < DEFAULT_NIGHT_END_HOUR
    }

    /** Результат вибору: [index] — який текст показати, [remaining] — що лишилось у «мішку». */
    data class Pick(val index: Int, val remaining: List<Int>)

    /**
     * Витягує наступний текст із [remaining] (індекси, ще не показані в цьому колі). Денні тексти
     * ніколи не потрапляють у нічний показ навпаки: НІЧНІ індекси допускаються лише коли [isNight].
     * Якщо з того, що лишилось, показувати нічого (мішок порожній або лишились самі нічні вдень) —
     * мішок наповнюється заново.
     */
    fun pick(remaining: List<Int>, isNight: Boolean, random: Random = Random.Default): Pick {
        fun allowed(list: List<Int>) = list.filter { isNight || it !in NIGHT_INDICES }
        var pool = remaining.filter { it in 0 until TOTAL }.distinct()
        if (allowed(pool).isEmpty()) pool = (0 until TOTAL).toList()
        val candidates = allowed(pool)
        val chosen = candidates[random.nextInt(candidates.size)]
        return Pick(chosen, pool - chosen)
    }

    fun encode(remaining: List<Int>): String = remaining.joinToString(",")

    /** Порожній/пошкоджений рядок — порожній «мішок» (наповниться при першому виборі). */
    fun decode(text: String?): List<Int> =
        text.orEmpty().split(",").mapNotNull { it.trim().toIntOrNull() }
}

/**
 * CC-6: опційна «зростаюча» затримка (вимкнена за замовчуванням): +[STEP_SECONDS] с за кожне повторне
 * відкриття тих самих воріт протягом [WINDOW_MILLIS], максимум [MAX_SECONDS] с; скидається після
 * [WINDOW_MILLIS] без відкриттів. Лічильник ніде не показується.
 */
object GrowingDelay {
    const val STEP_SECONDS = 5
    const val MAX_SECONDS = 20
    const val WINDOW_MILLIS = 30L * 60 * 1000

    /** Нова кількість повторних відкриттів: продовжує серію, якщо з попереднього показу минуло ≤ 30 хв. */
    fun repeatsAfterShow(previousRepeats: Int, lastShownMillis: Long, nowMillis: Long): Int =
        if (lastShownMillis > 0 && nowMillis - lastShownMillis <= WINDOW_MILLIS) previousRepeats + 1 else 0

    /** Затримка для цього показу: базова + крок × повтори, не більше [MAX_SECONDS] (і не менше базової). */
    fun effectiveSeconds(baseSeconds: Int, repeats: Int): Int =
        maxOf(baseSeconds, minOf(MAX_SECONDS, baseSeconds + STEP_SECONDS * repeats))
}
