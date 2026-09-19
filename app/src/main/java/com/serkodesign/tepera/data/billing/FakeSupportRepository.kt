package com.serkodesign.tepera.data.billing

import com.serkodesign.tepera.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Фейкові [SupportRepository] і [ProRepository] без RevenueCat — для розробки UI, прев'ю й тестів,
 * поки в дашборді нема продуктів. У продакшн-збірці не підключаються.
 */
class FakeSupportRepository(
    initial: SupportProductsState = SupportProductsState.Ready(
        listOf(
            SupportProduct("support_small", R.string.support_tier_small, "Small coffee", "$0.99"),
            SupportProduct("support_medium", R.string.support_tier_medium, "Coffee", "$2.99"),
            SupportProduct("support_large", R.string.support_tier_large, "Coffee and dessert", "$4.99")
        )
    ),
    private val purchaseResult: SupportPurchaseEvent = SupportPurchaseEvent.Thanks
) : SupportRepository {

    private val _products = MutableStateFlow(initial)
    override val products: StateFlow<SupportProductsState> = _products.asStateFlow()

    private val _events = MutableSharedFlow<SupportPurchaseEvent>(extraBufferCapacity = 4)
    override val events: SharedFlow<SupportPurchaseEvent> = _events.asSharedFlow()

    override fun refresh() = Unit

    override fun purchase(packageId: String) {
        _events.tryEmit(purchaseResult)
    }
}

class FakeProRepository(
    initial: ProState = ProState(configured = true, loaded = true, isPro = false),
    private val restoreOutcome: RestoreOutcome = RestoreOutcome.NothingToRestore
) : ProRepository {

    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<ProState> = _state.asStateFlow()

    override fun refresh() = Unit

    override suspend fun restore(): RestoreOutcome = restoreOutcome
}
