package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.dao.ObservationPeriodDao
import com.serkodesign.tepera.data.local.entity.ObservationPeriodEntity
import com.serkodesign.tepera.data.local.entity.RiskWindowEntity
import java.util.UUID
import com.serkodesign.tepera.domain.risk.RiskCandidate
import com.serkodesign.tepera.domain.risk.RiskConfig
import com.serkodesign.tepera.domain.risk.RiskResult
import com.serkodesign.tepera.domain.risk.RiskWindowFinder

/** Де лежать знімки тижневих перерахунків (у застосунку — DataStore, у тестах — пам'ять). */
interface ScanSnapshotStore {
    suspend fun load(): String
    suspend fun save(raw: String)
}

/** Звідки скан бере слоти: у застосунку — [UsageSlotRepository], у тестах — підміна. */
interface SlotSource {
    suspend fun refresh(nowMillis: Long): SlotRecompute
    suspend fun findWindows(cfg: RiskConfig, nowMillis: Long): RiskResult
}

sealed interface ScanOutcome {
    /** Нема списку стрічок або доступу до статистики. */
    data object Skipped : ScanOutcome

    /** Системні події не вдалось прочитати — нічого не збережено, повториться. */
    data object Failed : ScanOutcome

    data class NotEnoughData(val observedDays: Int) : ScanOutcome

    data class Done(val windows: List<RiskWindowEntity>, val shiftSuggestions: Int) : ScanOutcome
}

/**
 * v3.0 MVP, Етап 2 (п. 8–9): тижневий перерахунок — оновити слоти, знайти вікна, зберегти їх і перевірити, чи початок
 * відомих вікон не зсунувся два тижні поспіль. Зсув лише пропонується (поля `suggested*` вікна), межі мовчки не міняються.
 */
class RiskScanner(
    private val slots: SlotSource,
    private val windows: RiskWindowRepository,
    private val store: ScanSnapshotStore,
    private val periods: ObservationPeriodDao
) {
    suspend fun scan(cfg: RiskConfig = RiskConfig(), nowMillis: Long = System.currentTimeMillis()): ScanOutcome {
        when (slots.refresh(nowMillis)) {
            SlotRecompute.Skipped -> return ScanOutcome.Skipped
            SlotRecompute.Failed -> return ScanOutcome.Failed
            is SlotRecompute.Done -> Unit
        }
        val result = slots.findWindows(cfg, nowMillis)
        if (!result.enoughData) return ScanOutcome.NotEnoughData(result.observedDays)

        val saved = windows.saveDetected(result, cfg, nowMillis)
        // Базова лінія: тиждень, за який вікна знайдено вперше (планів ще нема, тож це «тиждень без втручань»).
        if (periods.getLatest() == null) {
            periods.upsert(ObservationPeriodEntity(UUID.randomUUID().toString(), result.days.first().toEpochDay(), result.days.last().toEpochDay()))
        }
        val history = RiskScanCodec.append(
            RiskScanCodec.decode(store.load()),
            ScanSnapshot(result.days.last().toEpochDay(), result.candidates)
        )
        store.save(RiskScanCodec.encode(history))

        // «Два тижні поспіль»: останні два знімки мають бути сусідніми тижнями, а не з розривом у місяць.
        val consecutive = history.size >= 2 && history.last().key - history[history.size - 2].key <= MAX_WEEK_GAP_DAYS
        val lastTwo = history.takeLast(2).map { it.candidates }
        var suggestions = 0
        for (w in saved.filter { !it.isUserDisabled && !it.isManual }) {
            val s = if (consecutive) RiskWindowFinder.suggestShift(w.toCandidate(), lastTwo, cfg) else null
            windows.setShiftSuggestion(w.id, s?.startMinute, s?.endMinute)
            if (s != null) suggestions++
        }
        return ScanOutcome.Done(windows.getAll(), suggestions)
    }

    private fun RiskWindowEntity.toCandidate() =
        RiskCandidate(startMinute, endMinute, recurrenceDays, observedDays, avgMinutes, emptyList(), dayType)

    private companion object {
        const val MAX_WEEK_GAP_DAYS = 10L
    }
}
