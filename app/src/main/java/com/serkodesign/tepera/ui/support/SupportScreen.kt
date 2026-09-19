package com.serkodesign.tepera.ui.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serkodesign.tepera.R
import com.serkodesign.tepera.data.billing.SupportProductsState
import com.serkodesign.tepera.data.billing.SupportPurchaseEvent
import com.serkodesign.tepera.data.billing.SupportRepository
import com.serkodesign.tepera.ui.theme.GlassRow
import com.serkodesign.tepera.ui.theme.GlassScreenHeader
import com.serkodesign.tepera.ui.theme.TeperaButton
import com.serkodesign.tepera.ui.theme.TeperaPalette

/**
 * "Пригостити розробника кавою" — добровільна підтримка через RevenueCat (пакети offering `support`,
 * [com.serkodesign.tepera.data.billing.RevenueCatConfig.OFFERING_SUPPORT]). Свідомо тихий екран
 * (Monastic Style): без бейджів "підтримав", без нагадувань і без тиску; нічого в застосунку не
 * розблоковується. Якщо offering ще не створений або RevenueCat не налаштований — спокійне
 * пояснення замість помилки. Інфраструктура — docs/revenuecat-setup.md.
 */
@Composable
fun SupportScreen(supportRepository: SupportRepository, onBack: () -> Unit) {
    val viewModel: SupportViewModel = viewModel(factory = SupportViewModel.Factory(supportRepository))
    val products by viewModel.products.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val message by viewModel.message.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            GlassScreenHeader(title = stringResource(R.string.support_screen_title), onBack = onBack)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.support_intro),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeperaPalette.buttonBrandDark
                )

                when (val state = products) {
                    SupportProductsState.Loading -> InfoCard(stringResource(R.string.support_loading))
                    SupportProductsState.Unavailable -> InfoCard(stringResource(R.string.support_unavailable))
                    is SupportProductsState.Ready -> {
                        state.products.forEach { product ->
                            val isSelected = product.packageId == selected
                            GlassRow(
                                label = product.labelRes?.let { stringResource(it) } ?: product.title,
                                onClick = { viewModel.select(product.packageId) },
                                leading = {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = TeperaPalette.buttonBrand
                                    )
                                },
                                trailing = {
                                    Text(
                                        text = product.priceText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TeperaPalette.buttonBrandDark
                                    )
                                }
                            )
                        }
                        TeperaButton(
                            text = stringResource(R.string.support_action),
                            onClick = viewModel::purchase,
                            enabled = selected != null,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }

                when (message) {
                    SupportPurchaseEvent.Thanks -> InfoCard(stringResource(R.string.support_thanks))
                    SupportPurchaseEvent.Pending -> InfoCard(stringResource(R.string.support_pending))
                    SupportPurchaseEvent.Failed -> InfoCard(stringResource(R.string.support_failed))
                    else -> Unit
                }
            }
        }
    }
}

/** Спокійна інформаційна картка (білий 80%, як картки Home). */
@Composable
private fun InfoCard(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .padding(12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = TeperaPalette.buttonBrandDark
    )
}
