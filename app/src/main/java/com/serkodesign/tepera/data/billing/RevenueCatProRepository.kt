package com.serkodesign.tepera.data.billing

import android.util.Log
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.StoreProduct
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * [ProRepository] на RevenueCat. Джерело істини про Pro — `CustomerInfo.entitlements[tepera_pro]`
 * (не окремі product id): так lifetime/yearly/monthly, промо й відновлення працюють однаково, а
 * набір продуктів змінюється в дашборді без релізу. Оновлення приходять двома шляхами: делегат
 * `onCustomerInfoUpdated` (після покупки в Paywall/Customer Center чи з іншого пристрою) і явний
 * [refresh]. Дані кешує сам SDK, тож застосунок працює й без мережі (останнім відомим станом).
 */
class RevenueCatProRepository(private val configured: Boolean) : ProRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(ProState(configured = configured))
    override val state: StateFlow<ProState> = _state.asStateFlow()

    init {
        if (configured && Purchases.isConfigured) {
            Purchases.sharedInstance.delegate = object : PurchasesDelegate {
                override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
                    _state.value = customerInfo.toProState()
                }

                override fun onPurchasePromoProduct(
                    product: StoreProduct,
                    startPurchase: (onError: (com.revenuecat.purchases.kmp.models.PurchasesError, userCancelled: Boolean) -> Unit,
                        onSuccess: (com.revenuecat.purchases.kmp.models.StoreTransaction, CustomerInfo) -> Unit) -> Unit
                ) = Unit // Apple-only сценарій; на Android не виникає.
            }
            refresh()
        }
    }

    override fun refresh() {
        if (!configured || !Purchases.isConfigured) return
        scope.launch {
            try {
                _state.value = Purchases.sharedInstance.awaitCustomerInfo().toProState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: PurchasesException) {
                Log.w(TAG, "customerInfo failed: ${e.error}")
                _state.value = _state.value.copy(loadFailed = true)
            }
        }
    }

    override suspend fun restore(): RestoreOutcome {
        if (!configured || !Purchases.isConfigured) return RestoreOutcome.Failed
        return try {
            val info = Purchases.sharedInstance.awaitRestore()
            val newState = info.toProState()
            _state.value = newState
            if (newState.isPro) RestoreOutcome.ProRestored else RestoreOutcome.NothingToRestore
        } catch (e: CancellationException) {
            throw e
        } catch (e: PurchasesException) {
            Log.w(TAG, "restore failed: ${e.error}")
            RestoreOutcome.Failed
        }
    }

    private fun CustomerInfo.toProState(): ProState {
        val pro = entitlements[RevenueCatConfig.ENTITLEMENT_PRO]?.takeIf { it.isActive }
        return ProState(
            configured = true,
            loaded = true,
            isPro = pro != null,
            productId = pro?.productIdentifier,
            expiresAtMillis = pro?.expirationDateMillis,
            willRenew = pro?.willRenew == true
        )
    }

    private companion object {
        const val TAG = "ProRepository"
    }
}
