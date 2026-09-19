package com.serkodesign.tepera.data.billing

import android.util.Log
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import com.revenuecat.purchases.kmp.models.PurchasesException
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * [SupportRepository] на RevenueCat: пакети беруться з offering [RevenueCatConfig.OFFERING_SUPPORT].
 * Споживні продукти RevenueCat споживає сам (без ручного `consumePurchase`); сервера й перевірки
 * покупки нема — нічого в застосунку не розблоковується. SDK налаштовується ліниво лише при першому
 * [refresh] (відкриття екрана підтримки), тож до цього застосунок не звертається до RevenueCat.
 */
class RevenueCatSupportRepository(private val ensureConfigured: () -> Boolean) : SupportRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _products = MutableStateFlow<SupportProductsState>(SupportProductsState.Loading)
    override val products: StateFlow<SupportProductsState> = _products.asStateFlow()

    private val _events = MutableSharedFlow<SupportPurchaseEvent>(extraBufferCapacity = 4)
    override val events: SharedFlow<SupportPurchaseEvent> = _events.asSharedFlow()

    private var packagesById: Map<String, Package> = emptyMap()

    override fun refresh() {
        // Ліниве налаштування SDK: перше відкриття екрана кави (див. TeperaApp.ensureRevenueCatConfigured).
        if (!ensureConfigured() || !Purchases.isConfigured) {
            _products.value = SupportProductsState.Unavailable
            return
        }
        scope.launch {
            _products.value = SupportProductsState.Loading
            try {
                val offering = Purchases.sharedInstance.awaitOfferings().all[RevenueCatConfig.OFFERING_SUPPORT]
                val packages = offering?.availablePackages.orEmpty()
                packagesById = packages.associateBy { it.identifier }
                _products.value = if (packages.isEmpty()) {
                    SupportProductsState.Unavailable
                } else {
                    SupportProductsState.Ready(
                        packages.map { pkg ->
                            SupportProduct(
                                packageId = pkg.identifier,
                                labelRes = SupportPackages.labelFor(pkg.identifier),
                                title = pkg.storeProduct.title,
                                priceText = pkg.storeProduct.price.formatted
                            )
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: PurchasesException) {
                Log.w(TAG, "offerings failed: ${e.error}")
                _products.value = SupportProductsState.Unavailable
            }
        }
    }

    override fun purchase(packageId: String) {
        val pkg = packagesById[packageId]
        if (pkg == null || !Purchases.isConfigured) {
            _events.tryEmit(SupportPurchaseEvent.Failed)
            return
        }
        scope.launch {
            try {
                Purchases.sharedInstance.awaitPurchase(pkg)
                _events.emit(SupportPurchaseEvent.Thanks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: PurchasesTransactionException) {
                _events.emit(
                    when {
                        e.userCancelled -> SupportPurchaseEvent.Cancelled
                        e.code == PurchasesErrorCode.PaymentPendingError -> SupportPurchaseEvent.Pending
                        else -> SupportPurchaseEvent.Failed
                    }
                )
            } catch (e: PurchasesException) {
                Log.w(TAG, "purchase failed: ${e.error}")
                _events.emit(SupportPurchaseEvent.Failed)
            }
        }
    }

    private companion object {
        const val TAG = "SupportRepository"
    }
}
