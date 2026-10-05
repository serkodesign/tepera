package com.serkodesign.tepera.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.serkodesign.tepera.MainActivity
import com.serkodesign.tepera.R

/** Екран, куди веде ярлик (без залежності від приватних маршрутів NavHost). */
enum class ShortcutScreen { DIARY, STATS }

/**
 * Ярлики застосунку (довгий тап по іконці Tepera): "Пауза затримки" / "Відновити затримку",
 * "Щоденник", "Статистика".
 *
 * Динамічні, а не статичні (`res/xml/shortcuts.xml`): статичний ярлик не може змінити назву й
 * іконку зі станом паузи. Перебудовуються з [refresh] при запуску/поверненні в застосунок і після
 * тапу по ярлику паузи. Назва паузи може відставати, поки Tepera не відкривали (пауза минула сама),
 * тож дія ярлика завжди вирішує стан у момент тапу — див. `GateRepository.toggleTodayPause()`.
 */
object AppShortcuts {

    const val ACTION_PAUSE_TOGGLE = "com.serkodesign.tepera.shortcut.PAUSE_TOGGLE"
    const val ACTION_DIARY = "com.serkodesign.tepera.shortcut.DIARY"
    const val ACTION_STATS = "com.serkodesign.tepera.shortcut.STATS"

    private const val ID_PAUSE = "tepera_pause_gates"
    private const val ID_DIARY = "tepera_diary"
    private const val ID_STATS = "tepera_stats"

    /** Перебудовує всі три ярлики під поточну мову застосунку та стан паузи воріт. */
    fun refresh(context: Context, gatePaused: Boolean) {
        val pauseLabel = context.getString(
            if (gatePaused) R.string.shortcut_resume_gates else R.string.shortcut_pause_gates
        )
        val pauseIcon = if (gatePaused) R.drawable.ic_shortcut_resume else R.drawable.ic_shortcut_pause
        val shortcuts = listOf(
            shortcut(context, ID_PAUSE, pauseLabel, pauseIcon, ACTION_PAUSE_TOGGLE, rank = 0),
            shortcut(
                context, ID_DIARY, context.getString(R.string.diary_screen_title),
                R.drawable.ic_shortcut_diary, ACTION_DIARY, rank = 1
            ),
            shortcut(
                context, ID_STATS, context.getString(R.string.stats_screen_title),
                R.drawable.ic_shortcut_stats, ACTION_STATS, rank = 2
            )
        )
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    private fun shortcut(
        context: Context,
        id: String,
        label: String,
        iconRes: Int,
        action: String,
        rank: Int
    ): ShortcutInfoCompat = ShortcutInfoCompat.Builder(context, id)
        .setShortLabel(label)
        .setLongLabel(label)
        .setIcon(IconCompat.createWithResource(context, iconRes))
        .setIntent(Intent(context, MainActivity::class.java).setAction(action))
        .setRank(rank)
        .build()
}
