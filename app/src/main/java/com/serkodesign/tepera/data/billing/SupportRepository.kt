package com.serkodesign.tepera.data.billing

import androidx.annotation.StringRes
import com.serkodesign.tepera.R
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Пакет підтримки з offering `support`. Екран будується від ПАКЕТІВ offering, а не від жорстких
 * id продуктів: складові кави можна замінити (навіть на підписку "щомісячна підтримка") зміною
 * продуктів у дашборді RevenueCat, без нового релізу. [packageId] — RevenueCat package identifier;
 * [labelRes] — локалізована назва для відомих identifier ([SupportPackages]), інакше показується
 * [title] зі стору.
 */
data class SupportProduct(
    val packageId: String,
    @StringRes val labelRes: Int?,
    val title: String,
    val priceText: String
)

/** Відомі package identifier рівнів кави → локалізовані назви (нові identifier працюють через title). */
object SupportPackages {
    fun labelFor(packageId: String): Int? = when (packageId) {
        "support_small" -> R.string.support_tier_small
        "support_medium" -> R.string.support_tier_medium
        "support_large" -> R.string.support_tier_large
        else -> null
    }
}

sealed interface SupportProductsState {
    /** Ще не запитували або запит іде. */
    data object Loading : SupportProductsState

    /**
     * RevenueCat не налаштований, нема мережі або offering `support` ще не створений/порожній —
     * екран показує спокійне пояснення, без помилки й без повторних спроб.
     */
    data object Unavailable : SupportProductsState

    data class Ready(val products: List<SupportProduct>) : SupportProductsState
}

/** Результат спроби підтримки — одноразова подія для екрана. */
enum class SupportPurchaseEvent { Thanks, Pending, Cancelled, Failed }

/**
 * Абстракція над RevenueCat для добровільної підтримки (Repository-патерн проєкту: ViewModel не
 * торкається SDK). Запити до RevenueCat відбуваються лише коли користувач відкриває екран підтримки.
 */
interface SupportRepository {
    val products: StateFlow<SupportProductsState>
    val events: SharedFlow<SupportPurchaseEvent>

    /** Запитати offering `support`. */
    fun refresh()

    /** Купити пакет [packageId]; результат приходить у [events]. */
    fun purchase(packageId: String)
}
