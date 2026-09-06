package com.serkodesign.tepera.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.serkodesign.tepera.MainActivity
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.util.startOfTodayMillis
import kotlinx.coroutines.flow.first

/**
 * FR-4.1–4.6: компактна 4x1 (5 кнопок категорій) і розширена 4x2 (+ шкала балансу) через
 * SizeMode.Responsive. FR-4.2: жодного live-таймера — стан статичний, оновлюється лише при
 * provideGlance() (тап по віджету, ручний resize, WidgetUpdateWorker ~30 хв).
 */
class TeperaWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 60.dp),
            DpSize(250.dp, 120.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TeperaApp

        val activeCategories = app.categoryRepository.observeActiveCategories().first()
        val sorted = sortCategoriesForWidget(activeCategories)

        // FR-4.6: перша занедбана категорія (>3 дні без запису) серед показаних кнопок.
        val neglectedCategoryId = sorted.take(5).firstOrNull { category ->
            isNeglected(app.activityRepository.lastLoggedTime(category.id))
        }?.id

        val hasUsageAccess = app.balanceRepository.hasUsageAccess()
        val onlineMinutes = if (hasUsageAccess) app.balanceRepository.getOnlineMinutesToday() else 0
        val denominatorMinutes = app.balanceRepository.calculateDenominatorMinutes()
        val targetMinutes = app.settingsStore.targetMinutes.first()
        val offlineMinutes = app.activityRepository
            .observeEntriesInRange(startOfTodayMillis(), Long.MAX_VALUE)
            .first()
            .sumOf { it.durationMinutes }

        provideContent {
            GlanceTheme {
                val isExtended = LocalSize.current.height >= 100.dp

                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.background)
                        .cornerRadius(16.dp)
                        .padding(8.dp)
                ) {
                    CategoryButtonsRow(
                        categories = sorted.take(5),
                        neglectedCategoryId = neglectedCategoryId,
                        context = context
                    )
                    if (isExtended) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        BalanceRow(
                            hasUsageAccess = hasUsageAccess,
                            onlineMinutes = onlineMinutes,
                            offlineMinutes = offlineMinutes,
                            targetMinutes = targetMinutes,
                            denominatorMinutes = denominatorMinutes,
                            context = context
                        )
                    }
                }
            }
        }
    }
}

private val MAX_BUTTON_SIZE = 48.dp // FR-4.1: hit-box >=48x48dp
private val BUTTON_GAP = 4.dp
private val BUTTON_RING_INSET = 8.dp

@Composable
private fun CategoryButtonsRow(
    categories: List<CategoryEntity>,
    neglectedCategoryId: String?,
    context: Context
) {
    // Реальна ширина, яку дає launcher, не завжди збігається з нашими DpSize-кандидатами
    // (targetCellWidth залежить від конкретного launcher-а) — тому розмір кнопки рахуємо
    // від фактичної LocalSize.current.width, а не жорстко фіксуємо 48dp: інакше 5 кнопок
    // по 48dp можуть не влізти й обрізатись праворуч замість акуратного зменшення.
    val count = categories.size.coerceAtLeast(1)
    val availableWidth = LocalSize.current.width - BUTTON_GAP * (count - 1)
    val buttonSize = (availableWidth / count).coerceIn(1.dp, MAX_BUTTON_SIZE)

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        categories.forEachIndexed { index, category ->
            if (index > 0) Spacer(modifier = GlanceModifier.width(BUTTON_GAP))
            CategoryButton(
                category = category,
                isNeglected = category.id == neglectedCategoryId,
                context = context,
                size = buttonSize
            )
        }
    }
}

@Composable
private fun CategoryButton(
    category: CategoryEntity,
    isNeglected: Boolean,
    context: Context,
    size: Dp
) {
    val label = categoryDisplayName(category, context)
    val initial = label.take(1).uppercase()
    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtra(MainActivity.EXTRA_CATEGORY_ID, category.id)
    }

    // FR-4.6: занедбана категорія (>3 дні без запису) отримує підсвічене кільце навколо кружка.
    Box(
        modifier = GlanceModifier
            .size(size)
            .then(
                if (isNeglected) {
                    GlanceModifier
                        .background(GlanceTheme.colors.primary)
                        .cornerRadius(size / 2)
                } else {
                    GlanceModifier
                }
            )
            .clickable(actionStartActivity(intent)),
        contentAlignment = Alignment.Center
    ) {
        val color = categoryColor(category.colorHex)
        val innerSize = (size - BUTTON_RING_INSET).coerceAtLeast(1.dp)
        Box(
            modifier = GlanceModifier
                .size(innerSize)
                .background(ColorProvider(day = color, night = color))
                .cornerRadius(innerSize / 2),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = TextStyle(
                    color = ColorProvider(day = Color.White, night = Color.White),
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun BalanceRow(
    hasUsageAccess: Boolean,
    onlineMinutes: Int,
    offlineMinutes: Int,
    targetMinutes: Int,
    denominatorMinutes: Int,
    context: Context
) {
    if (!hasUsageAccess) {
        Text(
            text = context.getString(R.string.usage_access_prompt_title),
            style = TextStyle(color = GlanceTheme.colors.onBackground)
        )
        return
    }

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = context.getString(R.string.balance_online_label) + ": " +
                    context.getString(R.string.minutes_short_format, onlineMinutes),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = GlanceTheme.colors.onBackground)
            )
            Text(
                text = context.getString(R.string.balance_offline_label) + ": " +
                    context.getString(R.string.minutes_short_format, offlineMinutes),
                style = TextStyle(color = GlanceTheme.colors.onBackground)
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        GlanceBalanceBar(
            onlineRatio = onlineMinutes.toFloat() / denominatorMinutes.coerceAtLeast(1),
            targetRatio = targetMinutes.toFloat() / denominatorMinutes.coerceAtLeast(1)
        )
    }
}

// Glance/RemoteViews не має Canvas і GlanceModifier.defaultWeight() не приймає довільну вагу
// (лише рівний розподіл) — на відміну від BalanceCard у застосунку (Compose Canvas), тому
// шкалу тут імітуємо решіткою з фіксованої кількості РІВНИХ за вагою сегментів: кожен сегмент
// пофарбований залежно від того, чи він у межах online-заповнення, і один сегмент — засічка
// таргету. 20 сегментів дають ~5% роздільної здатності, достатньо для розміру віджета.
private const val BALANCE_BAR_SEGMENTS = 20

/** FR-3.4/FR-4.1: та сама ідея, що BalanceCard у застосунку — заповнення + засічка таргету. */
@Composable
private fun GlanceBalanceBar(onlineRatio: Float, targetRatio: Float) {
    val filledCount = (onlineRatio.coerceIn(0f, 1f) * BALANCE_BAR_SEGMENTS)
        .toInt()
        .coerceIn(0, BALANCE_BAR_SEGMENTS)
    val markerIndex = (targetRatio.coerceIn(0f, 1f) * (BALANCE_BAR_SEGMENTS - 1))
        .toInt()
        .coerceIn(0, BALANCE_BAR_SEGMENTS - 1)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(10.dp)
            .cornerRadius(5.dp)
    ) {
        for (index in 0 until BALANCE_BAR_SEGMENTS) {
            if (index > 0) Spacer(modifier = GlanceModifier.width(1.dp))
            val segmentColor = when {
                index == markerIndex -> GlanceTheme.colors.error
                index < filledCount -> GlanceTheme.colors.primary
                else -> GlanceTheme.colors.secondaryContainer
            }
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .background(segmentColor)
            ) {}
        }
    }
}

class TeperaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TeperaWidget()
}
