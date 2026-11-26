package com.serkodesign.tepera.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.action.ActionParameters
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import com.serkodesign.tepera.BuildConfig
import com.serkodesign.tepera.MainActivity
import com.serkodesign.tepera.R
import com.serkodesign.tepera.TeperaApp
import com.serkodesign.tepera.data.DefaultCategories
import com.serkodesign.tepera.data.SuggestionPicker
import com.serkodesign.tepera.data.local.entity.CategoryEntity
import com.serkodesign.tepera.data.suggestionTextRes
import com.serkodesign.tepera.domain.risk.ActivitySuggestions
import com.serkodesign.tepera.domain.risk.PartOfDay
import com.serkodesign.tepera.ui.category.categoryColor
import com.serkodesign.tepera.ui.category.categoryDisplayName
import com.serkodesign.tepera.ui.risk.partOfDayRes
import com.serkodesign.tepera.util.AppShortcuts
import com.serkodesign.tepera.util.PlanOverlayStore
import com.serkodesign.tepera.util.WindowSkipStore
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

/**
 * Р’С–РґР¶РµС‚Рё РІ СЃС‚РёР»С– РѕСЃС‚Р°РЅРЅСЊРѕС— Р±РµС‚С‹ (РєР°РїСЃСѓР»Рё/РєР°СЂС‚РєРё Р· [infoColors], С‚РµРјРЅР°/СЃРІС–С‚Р»Р° С‚РµРјР°): В«РќР°СЃС‚СѓРїРЅР° РіРѕРґРёРЅР° РґРµС‚РѕРєСЃСѓВ», В«Р—Р°РјС–СЃС‚СЊ
 * СЃРєСЂРѕР»СѓВ», В«РўСЂРёРІР°С” Р·Р°СЂР°Р·В», В«РќРµ СЃСЊРѕРіРѕРґРЅС–В», В«РџР°СѓР·Р° Р·Р°С‚СЂРёРјРѕРєВ», В«Р”РѕРґР°С‚Рё Р·Р°РїРёСЃВ», В«Р¦РµР№ С‚РёР¶РґРµРЅСЊВ», В«РџР°СѓР·Рё Р±РµР· С‚РµР»РµС„РѕРЅСѓВ».
 *
 * Р§РѕС‚РёСЂРё Р· РЅРёС… Р·Р°Р»РµР¶Р°С‚СЊ РІС–Рґ РіРѕРґРёРЅ СЃРєСЂРѕР»С–РЅРіСѓ (РЅР°СЃС‚СѓРїРЅР° РіРѕРґРёРЅР°, С–РґРµСЏ, В«РЅРµ СЃСЊРѕРіРѕРґРЅС–В», С‚РёР¶РґРµРЅСЊ) вЂ” Сѓ Р·Р±С–СЂС†С– Р±РµР· СЂРµР¶РёРјСѓ
 * `V3_MVP` Р·Р°РјС–СЃС‚СЊ РґР°РЅРёС… РїРѕРєР°Р·СѓС”С‚СЊСЃСЏ Р¶Р°СЂС‚С–РІР»РёРІРёР№ РїР»РµР№СЃС…РѕР»РґРµСЂ ([HolidayPlaceholder]). В«Р¦РµР№ С‚РёР¶РґРµРЅСЊВ» С– В«РџР°СѓР·РёВ» РїРѕРєР°Р·СѓСЋС‚СЊ
 * С‡РёСЃР»Р°, С‰Рѕ РјРѕР¶СѓС‚СЊ Р·Р°СЃРјСѓС‚РёС‚Рё, С‚РѕР¶ РїР°СЃРёРІРЅРѕ Р·'СЏРІР»СЏСЋС‚СЊСЃСЏ Р»РёС€Рµ РїС–СЃР»СЏ СЏРІРЅРѕРіРѕ РІРІС–РјРєРЅРµРЅРЅСЏ РІ РЅР°Р»Р°С€С‚СѓРІР°РЅРЅСЏС… РІС–РґР¶РµС‚С–РІ.
 */

/** РЈСЃРµ, С‰Рѕ С‡РёС‚Р°СЋС‚СЊ РІС–РґР¶РµС‚Рё С†СЊРѕРіРѕ С„Р°Р№Р»Сѓ; Р·Р°Р№РІРµ РґР»СЏ РєРѕРЅРєСЂРµС‚РЅРѕРіРѕ РІС–РґР¶РµС‚Р° Р»РёС€Р°С”С‚СЊСЃСЏ РїРѕСЂРѕР¶РЅС–Рј ([ActionWidget.wants*]). */
internal data class ActionState(
    val v3: Boolean = BuildConfig.V3_MVP,
    val next: NextHourInfo? = null,
    val idea: ActivitySuggestions.Id? = null,
    val gatePaused: Boolean = false,
    val showWeek: Boolean = false,
    val week: WeekInfo? = null,
    val showPauses: Boolean = false,
    val pauses: Int = 0,
    val categories: List<CategoryEntity> = emptyList(),
    val timers: Map<String, Long> = emptyMap()
)

abstract class ActionWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    protected open val wantsNext: Boolean = false
    protected open val wantsIdea: Boolean = false
    protected open val wantsGate: Boolean = false
    protected open val wantsWeek: Boolean = false
    protected open val wantsPauses: Boolean = false

    @Composable
    internal abstract fun Content(context: Context, state: ActionState)

    /** Р”РµРјРѕ-РґР°РЅС– РґР»СЏ РјРµРЅСЋ РІС–РґР¶РµС‚С–РІ (Android 15+ РіРµРЅРµСЂСѓС” РїСЂРµРІ'СЋ С‚РёРј СЃР°РјРёРј [Content]). */
    internal open fun previewState(): ActionState = ActionState(v3 = true)

    /** РћРґРёРЅ Р·Р±С–Р№ РѕРєСЂРµРјРѕРіРѕ РґР¶РµСЂРµР»Р° РЅРµ РїРѕРІРёРЅРµРЅ СЂРѕР±РёС‚Рё РІС–РґР¶РµС‚ РїРѕСЂРѕР¶РЅС–Рј/РїРѕРјРёР»РєРѕРІРёРј вЂ” С†Рµ РґР¶РµСЂРµР»Рѕ РїСЂРѕСЃС‚Рѕ Р±РµР· РґР°РЅРёС…. */
    private suspend fun <T> safely(block: suspend () -> T): T? = try {
        block()
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private suspend fun load(context: Context, app: TeperaApp): ActionState {
        val v3 = BuildConfig.V3_MVP
        val next = if (v3 && wantsNext) safely { ActionWidgetData.nextHour(app) } else null
        val idea = if (v3 && wantsIdea) safely { WidgetIdeaStore.current(context) } else null
        val nextIdea = if (v3 && wantsNext && next != null) {
            safely { PlanOverlayStore.activeCommitment(context) ?: SuggestionPicker.pickFor(context, next.windowId)?.id }
        } else idea
        val gatePaused = if (wantsGate) safely { app.gateRepository.isPauseActive() } ?: false else false
        val showWeek = wantsWeek
        val week = if (v3 && wantsWeek && showWeek) safely { ActionWidgetData.week(app) } else null
        val showPauses = wantsPauses
        val pauses = if (wantsPauses && showPauses) safely { ActionWidgetData.unnamedPauses(app) } ?: 0 else 0
        return ActionState(
            v3 = v3, next = next, idea = nextIdea, gatePaused = gatePaused,
            showWeek = showWeek, week = week, showPauses = showPauses, pauses = pauses
        )
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TeperaApp
        // РџРѕС‡Р°С‚РєРѕРІС– Р·РЅР°С‡РµРЅРЅСЏ Р·Р°РІР°РЅС‚Р°Р¶СѓСЋС‚СЊСЃСЏ Р”Рћ РїРµСЂС€РѕРіРѕ РєР°РґСЂСѓ (РїСЂРёС‡РёРЅР° вЂ” Сѓ TeperaWidget.provideGlance).
        val initial = load(context, app).copy(
            categories = app.categoryRepository.observeActiveCategories().first(),
            timers = app.activeTimerStore.activeTimers.first()
        )
        val startTick = WidgetLiveData.refreshTick.value
        val startActionTick = WidgetLiveData.actionTick.value
        provideContent {
            val tick by WidgetLiveData.refreshTick.collectAsState()
            val actionTick by WidgetLiveData.actionTick.collectAsState()
            val categories by app.categoryRepository.observeActiveCategories().collectAsState(initial = initial.categories)
            val timers by app.activeTimerStore.activeTimers.collectAsState(initial = initial.timers)
            val loaded by produceState(initialValue = initial, tick, actionTick) {
                if (tick != startTick || actionTick != startActionTick) value = load(context, app)
            }
            GlanceTheme {
                Content(context, loaded.copy(categories = categories, timers = timers))
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { GlanceTheme { Content(context, previewState()) } }
    }
}

// ---------- СЃРїС–Р»СЊРЅС– РµР»РµРјРµРЅС‚Рё ----------

private fun openApp(context: Context): Action = actionStartActivity(Intent(context, MainActivity::class.java))

private fun clock(at: LocalDateTime): String = String.format(Locale.ROOT, "%02d:%02d", at.hour, at.minute)

/** РљСЂСѓР¶РѕРє С–Р· СЃРёРјРІРѕР»РѕРј (Рј'СЏС‚РЅР° Р·Р°Р»РёРІРєР° С‚РµРјРё). */
@Composable
private fun Badge(colors: InfoColors, iconRes: Int, size: Dp = 44.dp) {
    Box(modifier = GlanceModifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(R.drawable.widget_circle_solid),
            contentDescription = null,
            colorFilter = ColorFilter.tint(glanceColor(colors.mint)),
            modifier = GlanceModifier.size(size)
        )
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(glanceColor(colors.mintIcon)),
            modifier = GlanceModifier.size(size / 2)
        )
    }
}

/** Р—Р°РіРѕР»РѕРІРѕРє С– Р·РЅР°С‡РµРЅРЅСЏ РґРІРѕРјР° СЂСЏРґРєР°РјРё вЂ” СЃРїС–Р»СЊРЅР° Р±СѓРґРѕРІР° РєР°РїСЃСѓР». */
@Composable
private fun TwoLines(
    top: String,
    bottom: String,
    colors: InfoColors,
    modifier: GlanceModifier = GlanceModifier,
    third: String? = null,
    compact: Boolean = false
) {
    Column(modifier = modifier) {
        InfoText(top, if (compact) 11 else 12, colors.sub, FontWeight.Medium)
        InfoText(bottom, if (compact) 15 else 17, colors.text, FontWeight.Bold)
        if (third != null) InfoText(third, if (compact) 10 else 11, colors.sub, FontWeight.Medium)
    }
}

/** Р’СѓР·СЊРєРёР№ РІС–РґР¶РµС‚: СЃРёРјРІРѕР» Сѓ РєСЂСѓР¶РєСѓ РЅРµ РІРјС–С‰СѓС”С‚СЊСЃСЏ РїРѕСЂСѓС‡ С–Р· С‚РµРєСЃС‚РѕРј. */
private fun showBadge(width: Dp) = width >= 150.dp

/** РќР° РІСѓР·СЊРєРёС… РІС–РґР¶РµС‚Р°С… (2x1 РЅР° EMUI ~125dp) С‚РµРєСЃС‚ РґСЂС–Р±РЅС–С€РёР№ С– РєРѕСЂРѕС‚С€РёР№. */
private fun isNarrow(width: Dp) = width < 150.dp

/**
 * РљСЂСѓРіР»Р° РєРЅРѕРїРєР° 1x1 Р· СЃРёРјРІРѕР»РѕРј С– РєРѕСЂРѕС‚РєРёРј РїС–РґРїРёСЃРѕРј. [on] вЂ” Р·Р°Р»РёС‚Р° Р°РєС†РµРЅС‚РѕРј (РґС–СЏ РІРІС–РјРєРЅРµРЅР°), С–РЅР°РєС€Рµ РЅР°РїС–РІРїСЂРѕР·РѕСЂРёР№ РєСЂСѓРі С‚РµРјРё.
 * Р¤РѕРЅ вЂ” Р·Р°РІР¶РґРё С‚РѕР№ СЃР°РјРёР№ drawable, Р»РёС€Рµ С‚РѕРЅСѓРІР°РЅРЅСЏ Р·РјС–РЅСЋС”С‚СЊСЃСЏ (СѓРјРѕРІРЅРёР№ `background()` Сѓ RemoteViews РЅРµРЅР°РґС–Р№РЅРёР№).
 */
@Composable
private fun RoundButton(context: Context, colors: InfoColors, iconRes: Int, label: String, on: Boolean, action: Action) {
    val size = LocalSize.current
    val d = minOf(CAPSULE_HEIGHT, size.width, size.height)
    val content = if (on) colors.btnIcon else colors.text
    // РЈ РІСѓР·СЊРєРѕРјСѓ РєРѕР»С– (1x1 РЅР° EMUI ~50dp) РїС–РґРїРёСЃ РЅРµ РІРјС–С‰СѓС”С‚СЊСЃСЏ вЂ” Р»РёС€Р°С”С‚СЊСЃСЏ Р»РёС€Рµ СЃРёРјРІРѕР» (РѕРїРёСЃ вЂ” РґР»СЏ СЃРєСЂС–РЅСЂС–РґРµСЂР°).
    val showLabel = d >= 72.dp
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(modifier = GlanceModifier.size(d).clickable(action), contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(R.drawable.widget_circle_solid),
                contentDescription = null,
                colorFilter = ColorFilter.tint(glanceColor(if (on) colors.btn else colors.fill)),
                modifier = GlanceModifier.size(d)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = label,
                    colorFilter = ColorFilter.tint(glanceColor(content)),
                    modifier = GlanceModifier.size(if (showLabel) 24.dp else 26.dp)
                )
                if (showLabel) InfoText(label, 10, content, FontWeight.Bold)
            }
        }
    }
}

/**
 * Р–Р°СЂС‚С–РІР»РёРІРёР№ РїР»РµР№СЃС…РѕР»РґРµСЂ РґР»СЏ РІС–РґР¶РµС‚С–РІ, С‰Рѕ РїРѕС‚СЂРµР±СѓСЋС‚СЊ РіРѕРґРёРЅ СЃРєСЂРѕР»С–РЅРіСѓ, Сѓ Р·Р±С–СЂС†С– Р±РµР· РЅРёС…: РјС–СЃСЏС†СЊ Сѓ Рј'СЏС‚РЅРѕРјСѓ РєСЂСѓР¶РєСѓ Р№ РґРІР°
 * СЂСЏРґРєРё Р· СѓСЃРјС–С€РєРѕСЋ. Р’СѓР·СЊРєРёР№ РІС–РґР¶РµС‚ (1x1) вЂ” РєСЂСѓРі С–Р· В«ZzzвЂ¦В», РєР°СЂС‚РєР° (2x2) вЂ” РїРѕ С†РµРЅС‚СЂСѓ РєРѕР»РѕРЅРєРѕСЋ.
 */
@Composable
private fun HolidayPlaceholder(context: Context, colors: InfoColors, titleRes: Int, bodyRes: Int, card: Boolean = false) {
    val size = LocalSize.current
    val open = openApp(context)
    when {
        card -> Column(
            modifier = GlanceModifier.fillMaxSize().background(ImageProvider(colors.card)).padding(16.dp).clickable(open),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Badge(colors, R.drawable.ic_widget_info_moon, 48.dp)
            Spacer(modifier = GlanceModifier.height(12.dp))
            InfoText(context.getString(titleRes), 16, colors.text, FontWeight.Bold, maxLines = 2)
            Spacer(modifier = GlanceModifier.height(4.dp))
            InfoText(context.getString(bodyRes), 12, colors.sub, FontWeight.Medium, maxLines = 4)
        }
        size.width < 100.dp -> {
            val d = minOf(CAPSULE_HEIGHT, size.width, size.height)
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(modifier = GlanceModifier.size(d).clickable(open), contentAlignment = Alignment.Center) {
                    Image(
                        provider = ImageProvider(R.drawable.widget_circle_solid),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(glanceColor(colors.fill)),
                        modifier = GlanceModifier.size(d)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_info_moon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(glanceColor(colors.mintIcon)),
                            modifier = GlanceModifier.size(24.dp)
                        )
                        InfoText(context.getString(R.string.widget_ph_short), 11, colors.sub, FontWeight.Bold)
                    }
                }
            }
        }
        else -> Capsule(context, colors, 12.dp) {
            // Р’СѓР·СЊРєР° РєР°РїСЃСѓР»Р° (2x1 РЅР° EMUI ~125dp): Р±РµР· РєСЂСѓР¶РєР°, Р¶Р°СЂС‚ вЂ” РѕРґРЅРёРј СЂСЏРґРєРѕРј В«ZzzвЂ¦В» Р·Р°РјС–СЃС‚СЊ РґРѕРІРіРѕРіРѕ РїРѕСЏСЃРЅРµРЅРЅСЏ.
            val narrow = isNarrow(size.width)
            if (!narrow) {
                Badge(colors, R.drawable.ic_widget_info_moon)
                Spacer(modifier = GlanceModifier.width(10.dp))
            }
            Column(modifier = GlanceModifier.defaultWeight()) {
                InfoText(context.getString(titleRes), if (narrow) 13 else 14, colors.text, FontWeight.Bold, maxLines = 2)
                if (narrow) {
                    InfoText(context.getString(R.string.widget_ph_short), 11, colors.sub, FontWeight.Medium)
                } else {
                    InfoText(context.getString(bodyRes), 12, colors.sub, FontWeight.Medium, maxLines = 2)
                }
            }
        }
    }
}

/** РџРѕСЏСЃРЅРµРЅРЅСЏ В«С‡РёСЃР»Р° РІРёРјРєРЅРµРЅС–В» вЂ” РЅР° РІС–РґР¶РµС‚Р°С…, С‰Рѕ РїРѕРєР°Р·СѓСЋС‚СЊ РјРµС‚СЂРёРєРё Р»РёС€Рµ РїС–СЃР»СЏ РІРІС–РјРєРЅРµРЅРЅСЏ. */
@Composable
private fun HiddenMetric(context: Context, colors: InfoColors, iconRes: Int) {
    Capsule(context, colors, 12.dp) {
        if (showBadge(LocalSize.current.width)) {
            Badge(colors, iconRes)
            Spacer(modifier = GlanceModifier.width(10.dp))
        }
        val narrow = isNarrow(LocalSize.current.width)
        Column(modifier = GlanceModifier.defaultWeight()) {
            InfoText(context.getString(R.string.widget_metric_hidden_title), if (narrow) 13 else 14, colors.text, FontWeight.Bold)
            InfoText(context.getString(R.string.widget_metric_hidden_hint), if (narrow) 11 else 12, colors.sub, FontWeight.Medium, maxLines = 2)
        }
    }
}

// ---------- 1. РќР°СЃС‚СѓРїРЅР° РіРѕРґРёРЅР° РґРµС‚РѕРєСЃСѓ (2x1 / 4x1) ----------

class NextHourWidget : ActionWidget() {
    override val wantsNext = true

    override fun previewState(): ActionState {
        val start = LocalDateTime.now().withHour(21).withMinute(0).withSecond(0).withNano(0)
        return ActionState(
            v3 = true,
            next = NextHourInfo("preview", PartOfDay.EVENING, start, start.plusHours(2), inside = false, tomorrow = false, skipped = false, startMinute = 21 * 60, endMinute = 23 * 60),
            idea = ActivitySuggestions.Id.BOOK
        )
    }

    @Composable
    override fun Content(context: Context, state: ActionState) {
        val colors = infoColors(context)
        if (!state.v3) {
            HolidayPlaceholder(context, colors, R.string.widget_ph_next_title, R.string.widget_ph_next_body)
            return
        }
        val width = LocalSize.current.width
        val withIdea = width >= 260.dp && state.idea != null
        val compact = isNarrow(width) || withIdea
        // Р“РѕРґРёРЅРё Р±РµР· С…РІРёР»РёРЅ: Сѓ РІСѓР·СЊРєС–Р№ РєР°РїСЃСѓР»С– (2x1) вЂ” РІС–Рґ РіРѕРґРёРЅРё, Сѓ С€РёСЂРѕРєС–Р№ С–Р· РїС–РґРєР°Р·РєРѕСЋ вЂ” РІС–Рґ С‚СЂСЊРѕС… РіРѕРґРёРЅ.
        val hoursOnlyFrom = if (isNarrow(width)) 60 else if (withIdea) 180 else Int.MAX_VALUE
        val (top, bottom) = nextTexts(context, state.next, hoursOnlyFrom)
        Capsule(context, colors, 12.dp) {
            if (showBadge(width)) {
                Badge(colors, R.drawable.ic_widget_info_schedule)
                Spacer(modifier = GlanceModifier.width(10.dp))
            }
            TwoLines(top, bottom, colors, GlanceModifier.defaultWeight(), compact = compact)
            // РЁРёСЂРѕРєРёР№ РІС–РґР¶РµС‚ (4x1) РґРѕРґР°С” С‰Рµ Р№ РїС–РґРєР°Р·РєСѓ, С‰Рѕ СЂРѕР±РёС‚Рё Р·Р°РјС–СЃС‚СЊ СЃС‚СЂС–С‡РєРё.
            val idea = state.idea
            if (width >= 260.dp && idea != null) {
                Spacer(modifier = GlanceModifier.width(10.dp))
                InfoText(context.getString(suggestionTextRes(idea)), 13, colors.text, FontWeight.Medium, GlanceModifier.defaultWeight(), maxLines = 3)
            }
        }
    }
}

private fun nextTexts(context: Context, info: NextHourInfo?, hoursOnlyFrom: Int): Pair<String, String> {
    if (info == null) return context.getString(R.string.widget_next_none_title) to context.getString(R.string.widget_next_none)
    val part = context.getString(partOfDayRes(info.part))
    return when {
        info.inside -> context.getString(R.string.v2_next_title_inside, part, clock(info.endAt)) to context.getString(R.string.widget_next_now)
        info.skipped -> context.getString(R.string.v2_next_title_upcoming, part, clock(info.startAt)) to context.getString(R.string.widget_next_skipped)
        info.tomorrow -> part to context.getString(R.string.widget_next_tomorrow_format, clock(info.startAt))
        else -> {
            val minutes = Duration.between(LocalDateTime.now(), info.startAt).toMinutes().coerceAtLeast(1).toInt()
            context.getString(R.string.v2_next_title_upcoming, part, clock(info.startAt)) to
                context.getString(R.string.widget_next_in_format, formatDuration(context, if (minutes >= hoursOnlyFrom) minutes - minutes % 60 else minutes))
        }
    }
}

// ---------- 2. Р—Р°РјС–СЃС‚СЊ СЃРєСЂРѕР»Сѓ (2x2) ----------

class IdeaWidget : ActionWidget() {
    override val wantsIdea = true

    override fun previewState() = ActionState(v3 = true, idea = ActivitySuggestions.Id.WALK)

    @Composable
    override fun Content(context: Context, state: ActionState) {
        val colors = infoColors(context)
        if (!state.v3) {
            HolidayPlaceholder(context, colors, R.string.widget_ph_idea_title, R.string.widget_ph_idea_body, card = true)
            return
        }
        val text = state.idea?.let { context.getString(suggestionTextRes(it)) } ?: context.getString(R.string.widget_idea_fallback)
        Column(
            modifier = GlanceModifier.fillMaxSize().background(ImageProvider(colors.card)).padding(16.dp)
                .clickable(openApp(context))
        ) {
            InfoText(context.getString(R.string.widget_idea_header), 12, colors.sub, FontWeight.Medium)
            Spacer(modifier = GlanceModifier.height(8.dp))
            InfoText(text, 18, colors.text, FontWeight.Bold, GlanceModifier.defaultWeight(), maxLines = 4)
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Row(
                    modifier = GlanceModifier.background(ImageProvider(colors.chip))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clickable(actionRunCallback<IdeaAnotherAction>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_info_refresh),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(glanceColor(colors.mintIcon)),
                        modifier = GlanceModifier.size(16.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    InfoText(context.getString(R.string.widget_idea_other), 13, colors.mintIcon, FontWeight.Bold)
                }
            }
        }
    }
}

class IdeaAnotherAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Р†РґРµСЏ, РІС–Рґ СЏРєРѕС— Р»СЋРґРёРЅР° РІС–РґРјРѕРІРёР»Р°СЃСЊ РєРЅРѕРїРєРѕСЋ В«Р†РЅС€Р°В», РєС–Р»СЊРєР° РґРЅС–РІ РЅРµ РїРѕРІС‚РѕСЂСЋС”С‚СЊСЃСЏ вЂ” СЏРє Сѓ РїС–РґРєР°Р·РѕРє РїРѕРІРµСЂС… СЃС‚СЂС–С‡РєРё.
        WidgetIdeaStore.current(context)?.let { PlanOverlayStore.recordDeclined(context, it) }
        WidgetIdeaStore.another(context)
        updateAllActionWidgets(context)
    }
}

// ---------- 3. РўСЂРёРІР°С” Р·Р°СЂР°Р· (2x1) ----------

class RunningTimerWidget : ActionWidget() {

    override fun previewState(): ActionState {
        val reading = DefaultCategories.all.first { it.id == DefaultCategories.READING_ID }
        return ActionState(v3 = true, categories = listOf(reading), timers = mapOf(reading.id to 0L))
    }

    @Composable
    override fun Content(context: Context, state: ActionState) {
        val colors = infoColors(context)
        val running = state.categories.firstOrNull { it.id in state.timers }
        Capsule(context, colors, 12.dp) {
            if (running == null) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    InfoText(context.getString(R.string.widget_timer_idle), if (isNarrow(LocalSize.current.width)) 13 else 14, colors.text, FontWeight.Bold, maxLines = 2)
                    InfoText(context.getString(R.string.widget_timer_idle_hint), if (isNarrow(LocalSize.current.width)) 11 else 12, colors.sub, FontWeight.Medium)
                }
            } else {
                Dot(categoryColor(running.colorHex), 14.dp)
                Spacer(modifier = GlanceModifier.width(8.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    InfoText(categoryDisplayName(running, context), 15, colors.text, FontWeight.Bold)
                    InfoText(context.getString(R.string.widget_info_running), 12, colors.sub, FontWeight.Medium)
                }
                TimerButton(context, colors, running.id, running = true, size = 44.dp)
            }
        }
    }
}

// ---------- 4. РќРµ СЃСЊРѕРіРѕРґРЅС– (1x1) ----------

class QuietTodayWidget : ActionWidget() {
    override val wantsNext = true

    override fun previewState() = ActionState(
        v3 = true,
        next = LocalDateTime.now().let {
            NextHourInfo("preview", PartOfDay.EVENING, it.plusHours(2), it.plusHours(4), false, false, false, 21 * 60, 23 * 60)
        }
    )

    @Composable
    override fun Content(context: Context, state: ActionState) {
        val colors = infoColors(context)
        if (!state.v3) {
            HolidayPlaceholder(context, colors, R.string.widget_ph_quiet_title, R.string.widget_ph_quiet_body)
            return
        }
        val info = state.next
        when {
            info == null -> RoundButton(context, colors, R.drawable.ic_widget_info_quiet, context.getString(R.string.widget_quiet_none), false, openApp(context))
            info.inside -> RoundButton(context, colors, R.drawable.ic_widget_info_quiet, context.getString(R.string.widget_next_now), false, openApp(context))
            else -> RoundButton(
                context, colors, R.drawable.ic_widget_info_quiet,
                context.getString(if (info.skipped) R.string.widget_quiet_on else R.string.widget_quiet_off),
                info.skipped, actionRunCallback<QuietTodayAction>()
            )
        }
    }
}

/** РўРѕР№ СЃР°РјРёР№ РІРёР±С–СЂ, С‰Рѕ РєРЅРѕРїРєР° В«РќРµ СЃСЊРѕРіРѕРґРЅС–В» РЅР° РєР°СЂС‚С†С– РЅР°СЃС‚СѓРїРЅРѕС— РіРѕРґРёРЅРё: РІС–РєРЅРѕ РјРѕРІС‡РёС‚СЊ РґРѕ РєС–РЅС†СЏ РЅР°Р№Р±Р»РёР¶С‡РѕРіРѕ РїСЂРѕС…РѕРґСѓ. */
class QuietTodayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as TeperaApp
        val info = ActionWidgetData.nextHour(app) ?: return
        if (info.inside) return
        if (info.skipped) {
            WindowSkipStore.undo(context, info.windowId)
        } else {
            WindowSkipStore.skipUntil(context, info.windowId, info.endAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        }
        updateAllActionWidgets(context)
    }
}

// ---------- 5. РџР°СѓР·Р° Р·Р°С‚СЂРёРјРѕРє РЅР° СЃСЊРѕРіРѕРґРЅС– (1x1) ----------

class GatePauseWidget : ActionWidget() {
    override val wantsGate = true

    @Composable
    override fun Content(context: Context, state: ActionState) {
        val colors = infoColors(context)
        RoundButton(
            context, colors, R.drawable.ic_widget_info_hourglass,
            context.getString(if (state.gatePaused) R.string.widget_gate_paused else R.string.widget_gate_on),
            state.gatePaused, actionRunCallback<GatePauseTodayAction>()
        )
    }
}

/** РўР° СЃР°РјР° РґС–СЏ, С‰Рѕ СЏСЂР»РёРє В«РџР°СѓР·Р° Р·Р°С‚СЂРёРјРѕРєВ» С– РєРЅРѕРїРєР° РЅР° РµРєСЂР°РЅС– РІРѕСЂС–С‚: С‡РёРЅРЅР° РїР°СѓР·Р° вЂ” СЃРєР°СЃСѓРІР°С‚Рё, С–РЅР°РєС€Рµ вЂ” РїР°СѓР·Р° РЅР° СЃСЊРѕРіРѕРґРЅС–. */
class GatePauseTodayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val app = context.applicationContext as TeperaApp
        app.gateRepository.toggleTodayPause()
        AppShortcuts.refresh(context, app.gateRepository.isPauseActive())
        updateAllActionWidgets(context)
    }
}

// ---------- 6. Р”РѕРґР°С‚Рё Р·Р°РїРёСЃ (1x1 / 2x1) ----------

class AddEntryWidget : ActionWidget() {

    @Composable
    override fun Content(context: Context, state: ActionState) {
        val colors = infoColors(context)
        val open = actionStartActivity(Intent(context, MainActivity::class.java).setAction(AppShortcuts.ACTION_ADD_ENTRY))
        val width = LocalSize.current.width
        if (width < 110.dp) {
            // 1x1: РѕРґРЅР° Р·Р°Р»РёРІРЅР° РєРЅРѕРїРєР° В«+В».
            val d = minOf(CAPSULE_HEIGHT, width, LocalSize.current.height)
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(modifier = GlanceModifier.size(d).clickable(open), contentAlignment = Alignment.Center) {
                    Image(
                        provider = ImageProvider(R.drawable.widget_circle_solid),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(glanceColor(colors.btn)),
                        modifier = GlanceModifier.size(d)
                    )
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_info_add),
                        contentDescription = context.getString(R.string.widget_add_text),
                        colorFilter = ColorFilter.tint(glanceColor(colors.btnIcon)),
                        modifier = GlanceModifier.size(30.dp)
                    )
                }
            }
        } else {
            Capsule(context, colors, 14.dp, action = open) {
                Box(modifier = GlanceModifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Image(
                        provider = ImageProvider(R.drawable.widget_circle_solid),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(glanceColor(colors.btn)),
                        modifier = GlanceModifier.size(44.dp)
                    )
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_info_add),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(glanceColor(colors.btnIcon)),
                        modifier = GlanceModifier.size(24.dp)
                    )
                }
                Spacer(modifier = GlanceModifier.width(10.dp))
                InfoText(context.getString(R.string.widget_add_text), 16, colors.text, FontWeight.Bold, GlanceModifier.defaultWeight(), maxLines = 2)
            }
        }
    }
}

// ---------- 7. Р¦РµР№ С‚РёР¶РґРµРЅСЊ (4x1) ----------

class WeekWidget : ActionWidget() {
    override val wantsWeek = true

    override fun previewState() = ActionState(v3 = true, showWeek = true, week = WeekInfo(baseline = 95.0, current = 70.0, reference = false))

    @Composable
    override fun Content(context: Context, state: ActionState) {
        val colors = infoColors(context)
        if (!state.v3) {
            HolidayPlaceholder(context, colors, R.string.widget_ph_week_title, R.string.widget_ph_week_body)
            return
        }
        if (!state.showWeek) {
            HiddenMetric(context, colors, R.drawable.ic_widget_info_chart)
            return
        }
        val week = state.week
        val width = LocalSize.current.width
        Capsule(context, colors, 12.dp) {
            if (showBadge(width)) {
                Badge(colors, R.drawable.ic_widget_info_chart)
                Spacer(modifier = GlanceModifier.width(10.dp))
            }
            if (week == null) {
                TwoLines(
                    context.getString(R.string.widget_week_title), context.getString(R.string.widget_week_empty),
                    colors, GlanceModifier.defaultWeight()
                )
            } else {
                val minutes = (week.big ?: 0.0).toInt()
                TwoLines(
                    top = context.getString(if (week.reference) R.string.v2_week_reference_title else R.string.widget_week_title),
                    bottom = formatDuration(context, minutes),
                    colors = colors,
                    modifier = GlanceModifier.defaultWeight(),
                    third = context.getString(R.string.widget_week_unit)
                )
                // Р”РІС– СЃРјСѓРіРё РѕРґРЅРѕРіРѕ РјР°СЃС€С‚Р°Р±Сѓ (СЃРїРѕСЃС‚РµСЂРµР¶РµРЅРЅСЏ Р№ С‚РёР¶РґРµРЅСЊ) вЂ” Р»РёС€Рµ РєРѕР»Рё С” Р· С‡РёРј РїРѕСЂС–РІРЅСЋРІР°С‚Рё Р№ С” РјС–СЃС†Рµ.
                val baseline = week.baseline
                val current = week.current
                if (width >= 260.dp && !week.reference && baseline != null && current != null) {
                    val scale = maxOf(baseline, current, 1.0).toFloat()
                    val bars = renderWeekBars(
                        context.px(84.dp), context.px(28.dp), (baseline / scale).toFloat(), (current / scale).toFloat(),
                        colors.track.toArgb(), colors.sub.toArgb(), colors.btn.toArgb()
                    )
                    Spacer(modifier = GlanceModifier.width(10.dp))
                    Image(ImageProvider(bars), contentDescription = null, modifier = GlanceModifier.size(width = 84.dp, height = 28.dp))
                }
            }
        }
    }
}

// ---------- 8. РџР°СѓР·Рё Р±РµР· С‚РµР»РµС„РѕРЅСѓ (2x1) ----------

class PausesWidget : ActionWidget() {
    override val wantsPauses = true

    override fun previewState() = ActionState(v3 = true, showPauses = true, pauses = 2)

    @Composable
    override fun Content(context: Context, state: ActionState) {
        val colors = infoColors(context)
        if (!state.showPauses) {
            HiddenMetric(context, colors, R.drawable.ic_widget_info_pause)
            return
        }
        val width = LocalSize.current.width
        Capsule(context, colors, 12.dp) {
            if (showBadge(width)) {
                Badge(colors, R.drawable.ic_widget_info_pause)
                Spacer(modifier = GlanceModifier.width(10.dp))
            }
            if (state.pauses > 0) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    InfoText(context.getString(R.string.widget_pauses_count, state.pauses), if (isNarrow(width)) 15 else 17, colors.text, FontWeight.Bold)
                    InfoText(context.getString(R.string.widget_pauses_hint), if (isNarrow(width)) 11 else 12, colors.sub, FontWeight.Medium)
                }
            } else {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    InfoText(context.getString(R.string.widget_pauses_none), if (isNarrow(width)) 13 else 14, colors.text, FontWeight.Bold, maxLines = 2)
                    InfoText(context.getString(R.string.widget_pauses_none_hint), if (isNarrow(width)) 11 else 12, colors.sub, FontWeight.Medium)
                }
            }
        }
    }
}

// ---------- СЂРµСЃС–РІРµСЂРё Р№ РѕРЅРѕРІР»РµРЅРЅСЏ ----------

class NextHourWidgetReceiver : TrackedWidgetReceiver(com.serkodesign.tepera.analytics.WidgetKind.NEXT_HOUR) { override val glanceAppWidget: GlanceAppWidget = NextHourWidget() }
class IdeaWidgetReceiver : TrackedWidgetReceiver(com.serkodesign.tepera.analytics.WidgetKind.IDEA) { override val glanceAppWidget: GlanceAppWidget = IdeaWidget() }
class RunningTimerWidgetReceiver : TrackedWidgetReceiver(com.serkodesign.tepera.analytics.WidgetKind.ACTIVE_NOW) { override val glanceAppWidget: GlanceAppWidget = RunningTimerWidget() }
class QuietTodayWidgetReceiver : TrackedWidgetReceiver(com.serkodesign.tepera.analytics.WidgetKind.SKIP_TODAY) { override val glanceAppWidget: GlanceAppWidget = QuietTodayWidget() }
class GatePauseWidgetReceiver : TrackedWidgetReceiver(com.serkodesign.tepera.analytics.WidgetKind.GATE_PAUSE) { override val glanceAppWidget: GlanceAppWidget = GatePauseWidget() }
class AddEntryWidgetReceiver : TrackedWidgetReceiver(com.serkodesign.tepera.analytics.WidgetKind.ADD_ENTRY) { override val glanceAppWidget: GlanceAppWidget = AddEntryWidget() }
class WeekWidgetReceiver : TrackedWidgetReceiver(com.serkodesign.tepera.analytics.WidgetKind.WEEK) { override val glanceAppWidget: GlanceAppWidget = WeekWidget() }
class PausesWidgetReceiver : TrackedWidgetReceiver(com.serkodesign.tepera.analytics.WidgetKind.PAUSES) { override val glanceAppWidget: GlanceAppWidget = PausesWidget() }

/** РћРЅРѕРІР»СЋС” РІСЃС– РІС–РґР¶РµС‚Рё С†СЊРѕРіРѕ С„Р°Р№Р»Сѓ вЂ” РІРёРєР»РёРєР°С”С‚СЊСЃСЏ СЂР°Р·РѕРј Р· С–РЅС€РёРјРё С–РЅС„РѕСЂРјР°С†С–Р№РЅРёРјРё (РІРѕСЂРєРµСЂРё, С‚Р°Р№РјРµСЂРё) С– РїС–СЃР»СЏ РІР»Р°СЃРЅРёС… РґС–Р№. */
suspend fun updateAllActionWidgets(context: Context) {
    WidgetLiveData.actionTick.value++
    updateWidgetInstances(context, NextHourWidget(), NextHourWidgetReceiver::class.java)
    updateWidgetInstances(context, IdeaWidget(), IdeaWidgetReceiver::class.java)
    updateWidgetInstances(context, RunningTimerWidget(), RunningTimerWidgetReceiver::class.java)
    updateWidgetInstances(context, QuietTodayWidget(), QuietTodayWidgetReceiver::class.java)
    updateWidgetInstances(context, GatePauseWidget(), GatePauseWidgetReceiver::class.java)
    updateWidgetInstances(context, AddEntryWidget(), AddEntryWidgetReceiver::class.java)
    updateWidgetInstances(context, WeekWidget(), WeekWidgetReceiver::class.java)
    updateWidgetInstances(context, PausesWidget(), PausesWidgetReceiver::class.java)
}

/**
 * РћРЅРѕРІР»СЋС” РµРєР·РµРјРїР»СЏСЂРё СЃР°РјРµ Р¦Р¬РћР“Рћ РІС–РґР¶РµС‚Р°: appWidgetId Р±РµСЂСѓС‚СЊСЃСЏ РІ СЃРёСЃС‚РµРјРё Р·Р° Р№РѕРіРѕ СЂРµСЃС–РІРµСЂРѕРј, Р° РЅРµ Р·С– СЃС…РѕРІРёС‰Р° Glance
 * (`updateAll`) вЂ” РЅР° РїСЂРёСЃС‚СЂРѕС— (Huawei P9) РїС–СЃР»СЏ РѕРЅРѕРІР»РµРЅРЅСЏ Р·Р°СЃС‚РѕСЃСѓРЅРєСѓ `updateAll` РѕРґРЅРѕРіРѕ РєР»Р°СЃСѓ РїРµСЂРµРјР°Р»СЊРѕРІСѓРІР°РІ
 * РµРєР·РµРјРїР»СЏСЂРё С–РЅС€РёС… РІС–РґР¶РµС‚С–РІ, С– РІСЃС– РІРѕРЅРё РїРѕРєР°Р·СѓРІР°Р»Рё РІРјС–СЃС‚ В«РќР°СЃС‚СѓРїРЅРѕС— РіРѕРґРёРЅРёВ».
 */
private suspend fun updateWidgetInstances(context: Context, widget: GlanceAppWidget, receiver: Class<out GlanceAppWidgetReceiver>) {
    val ids = android.appwidget.AppWidgetManager.getInstance(context)
        .getAppWidgetIds(android.content.ComponentName(context, receiver))
    if (ids.isEmpty()) return
    val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
    ids.forEach { widget.update(context, manager.getGlanceIdBy(it)) }
}
