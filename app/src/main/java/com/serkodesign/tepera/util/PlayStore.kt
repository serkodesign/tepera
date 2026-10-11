package com.serkodesign.tepera.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * CC-9/CC-10: єдина точка відкриття лістингу Tepera в Play Store — м'яка пропозиція оновитись
 * ([com.serkodesign.tepera.ui.home.UpdateViewModel]) і пункт "Оцінити застосунок" (SettingsScreen)
 * ведуть туди тим самим кодом.
 *
 * За рекомендацією Google (Play In-App Review API — https://developer.android.com/guide/playcore/in-app-review)
 * пряме посилання на лістинг — правильний вибір саме для ЯВНОЇ дії користувача (пункт меню), НЕ
 * `ReviewManager`/In-App Review API: та бібліотека призначена для контекстних підказок у вдалий
 * момент use-flow, система сама квотує й вирішує, чи показати діалог (жодної гарантії показу за
 * один виклик), і Google прямо радить не підміняти нею пряме посилання з Налаштувань — "a direct
 * link to your app's Play Store listing... is an explicit indication of user intent and should
 * take users directly to the Play Store". Пункт у Налаштуваннях — саме такий явний запит.
 */
object PlayStore {
    /** [forReview] додає `showAllReviews=true` — лістинг одразу відкривається на розділі відгуків/оцінки. */
    fun openListing(context: Context, forReview: Boolean = false) {
        val suffix = if (forReview) "&showAllReviews=true" else ""
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}$suffix"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opened = runCatching { context.startActivity(marketIntent) }.isSuccess
        if (!opened) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}$suffix")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(webIntent) }
        }
    }
}
