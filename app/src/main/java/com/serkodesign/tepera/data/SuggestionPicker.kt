package com.serkodesign.tepera.data

import android.content.Context
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.domain.risk.ActivitySuggestions
import com.serkodesign.tepera.domain.risk.HeatGrid
import com.serkodesign.tepera.domain.risk.LogicalDay
import com.serkodesign.tepera.util.PlanOverlayStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Підбирає ОДНУ підказку для години ризику: пора доби береться з початку вікна, будень/вихідний — за логічною добою
 * (нічна година після півночі ще «вчорашня»), категорії — лише ті, що людина лишила активними.
 */
object SuggestionPicker {
    suspend fun pickFor(
        context: Context,
        windowId: String,
        exclude: Set<ActivitySuggestions.Id> = emptySet(),
        nowMillis: Long = System.currentTimeMillis()
    ): ActivitySuggestions.Suggestion? {
        val app = context.applicationContext as TeperaApp
        val window = app.riskWindowRepository.getWindow(windowId) ?: return null
        val available = app.categoryRepository.observeActiveCategories().first().map { it.id }.toSet()
        val weekend = LocalDate.ofEpochDay(LogicalDay.epochDay(nowMillis)).dayOfWeek.value > 5
        return ActivitySuggestions.pick(
            part = HeatGrid.partOfDay(window.startMinute),
            weekend = weekend,
            availableCategories = available,
            declined = PlanOverlayStore.declined(context),
            exclude = exclude,
            nowMillis = nowMillis
        )
    }
}

/** Текст пропозиції («Може, почитаєш книгу?») — для оверлея, сповіщення й екрана м'якого виходу. */
fun suggestionTextRes(id: ActivitySuggestions.Id): Int = when (id) {
    ActivitySuggestions.Id.BOOK -> R.string.v3_sg_book
    ActivitySuggestions.Id.PODCAST -> R.string.v3_sg_podcast
    ActivitySuggestions.Id.TODO_TOMORROW -> R.string.v3_sg_todo_tomorrow
    ActivitySuggestions.Id.CHARGE_PHONE -> R.string.v3_sg_charge_phone
    ActivitySuggestions.Id.BREATHE -> R.string.v3_sg_breathe
    ActivitySuggestions.Id.WALK -> R.string.v3_sg_walk
    ActivitySuggestions.Id.WATER -> R.string.v3_sg_water
    ActivitySuggestions.Id.STRETCH -> R.string.v3_sg_stretch
    ActivitySuggestions.Id.EAT_NO_PHONE -> R.string.v3_sg_eat_no_phone
    ActivitySuggestions.Id.CALL_FRIEND -> R.string.v3_sg_call_friend
    ActivitySuggestions.Id.HOBBY -> R.string.v3_sg_hobby
}
