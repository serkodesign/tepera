package com.serkodesign.tepera.ui.pro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.kmp.ui.revenuecatui.CustomerCenter
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions

/**
 * Paywall RevenueCat (дашборд → Paywalls) для current offering. Розкладка, тексти й пакети
 * (lifetime/yearly/monthly) задаються в дашборді без релізу застосунку. Закривається через
 * `dismissRequest` (кнопка закриття й автоматично після успішної покупки/відновлення); стан Pro
 * оновлює [com.serkodesign.tepera.data.billing.RevenueCatProRepository] через делегат SDK.
 */
@Composable
fun PaywallScreen(onClose: () -> Unit) {
    val options = remember {
        // Paywall сам викликає dismissRequest і після успішної покупки/відновлення, тому окремий
        // PaywallListener для закриття не потрібен (він давав подвійне закриття: два popBackStack).
        PaywallOptions(dismissRequest = onClose) {
            shouldDisplayDismissButton = true
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Paywall(options)
    }
}

/**
 * Customer Center RevenueCat: керування підпискою (скасування, зміна плану, повернення коштів,
 * відновлення) в одному екрані, налаштованому в дашборді (Customer Center). Показується з екрана Pro
 * лише коли Pro активний.
 */
@Composable
fun CustomerCenterScreen(onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        CustomerCenter(modifier = Modifier.fillMaxSize(), onDismiss = onClose)
    }
}
