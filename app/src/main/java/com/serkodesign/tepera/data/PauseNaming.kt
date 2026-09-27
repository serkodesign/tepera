package com.serkodesign.tepera.data

import com.serkodesign.tepera.data.local.entity.ActivityEntryEntity
import com.serkodesign.tepera.data.local.entity.DetectedGapEntity
import com.serkodesign.tepera.data.local.entity.EntrySource
import com.serkodesign.tepera.data.repository.ActivityRepository

/** Результат [namePause] — чи це перше називання цієї паузи, чи зміна вже наявної категорії. */
sealed class PauseNamingOutcome {
    data class Named(val entryId: String) : PauseNamingOutcome()
    data class Relabeled(val entryId: String) : PauseNamingOutcome()
}

/**
 * Вузький зріз `PauseRepository`, потрібний [namePause]/[undoPauseNaming] — той самий принцип, що
 * `CardHistorySource` для `CardEngine`: дозволяє тестувати логіку називання проти рукописного
 * фейка, не проти справжнього `PauseRepository` (той вимагає Android `Context` у конструкторі,
 * хоч ці конкретні методи його й не використовують). `PauseRepository` реалізує цей інтерфейс,
 * нічого додатково не змінюючи.
 */
interface PauseGapStore {
    suspend fun findOrCreateGap(startTime: Long, durationMinutes: Int): DetectedGapEntity
    suspend fun findGapByStartTime(startTime: Long): DetectedGapEntity?
    suspend fun markLabeledAt(gapId: String, entryId: String, atMillis: Long)
    suspend fun clearLabel(gapId: String)
}

/**
 * W-2 (`CLAUDE-CODE-TASK-widgets.md`), раніше відкладена задача CC-3: спільна логіка називання
 * паузи — використовується і Home (`PauseViewModel`), і майбутньою кнопкою категорії на віджеті
 * "Пульт" (W-4), той самий принцип, що [toggleCategoryTimer] для тап-таймера.
 *
 * Пауза ідентифікується своїми межами ([startTime]/[durationMinutes]), НЕ Room-рядком: рядок
 * непозначеної паузи перестворюється при кожному перескануванні
 * (`PauseRepository.persist`/`replaceUnresolvedInRange`, T-11), тож id, який віджет міг
 * закешувати до перескану, до моменту тапу вже міг не існувати — [PauseRepository.findOrCreateGap]
 * знаходить чи створює рядок наново з тих самих меж.
 *
 * **Повторне називання не дублює запис:** якщо пауза вже мала [labeledEntryId] і той запис ще
 * існує — категорія ОНОВЛЮЄТЬСЯ в тому самому записі ([PauseNamingOutcome.Relabeled]), а не
 * створюється другий поряд. Якщо той запис хтось видалив вручну (Щоденник) — рядок паузи лишається
 * "позначеним" формально, але без живого запису; тоді називаємо як уперше.
 */
suspend fun namePause(
    pauseRepository: PauseGapStore,
    activityRepository: ActivityRepository,
    startTime: Long,
    durationMinutes: Int,
    categoryId: String,
    nowMillis: Long = System.currentTimeMillis()
): PauseNamingOutcome {
    val gap = pauseRepository.findOrCreateGap(startTime, durationMinutes)
    val existingEntry = gap.labeledEntryId?.let { activityRepository.getById(it) }
    return if (existingEntry != null) {
        activityRepository.update(existingEntry.copy(categoryId = categoryId))
        pauseRepository.markLabeledAt(gap.id, existingEntry.id, nowMillis)
        PauseNamingOutcome.Relabeled(existingEntry.id)
    } else {
        val entry = ActivityEntryEntity(
            categoryId = categoryId,
            startTime = gap.startTime,
            durationMinutes = gap.durationMinutes,
            source = EntrySource.GAP_LABELED
        )
        // forceOverwrite: називання паузи — швидка дія без overlap-діалогу, той самий принцип, що тап-таймер.
        activityRepository.addEntry(entry, forceOverwrite = true)
        pauseRepository.markLabeledAt(gap.id, entry.id, nowMillis)
        PauseNamingOutcome.Named(entry.id)
    }
}

/**
 * W-2: скасування протягом 5 хв від МОМЕНТУ НАЗИВАННЯ (не від самої паузи) — повертає паузу в
 * повністю нейтральний стан (не до попередньої категорії, якщо це було перейменування): видаляє
 * створений/оновлений запис і чистить [labeledEntryId]/[labeledAtMillis]. `false`, якщо паузи з
 * такими межами нема, її ніколи не називали, чи вікно вже минуло — виклик не є помилкою, лише
 * "запізно", UI сам вирішує, що показати.
 */
suspend fun undoPauseNaming(
    pauseRepository: PauseGapStore,
    activityRepository: ActivityRepository,
    startTime: Long,
    nowMillis: Long = System.currentTimeMillis(),
    undoWindowMillis: Long = 5 * 60_000L
): Boolean {
    val gap = pauseRepository.findGapByStartTime(startTime) ?: return false
    val labeledAt = gap.labeledAtMillis ?: return false
    if (nowMillis - labeledAt > undoWindowMillis) return false
    gap.labeledEntryId?.let { entryId -> activityRepository.getById(entryId)?.let { activityRepository.delete(it) } }
    pauseRepository.clearLabel(gap.id)
    return true
}
