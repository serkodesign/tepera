package com.serkodesign.tepera.domain.berserk

/**
 * Чиста арифметика берсерк-режиму: скільки безперервного часу людина провела в стрічках. Без Android і без годинника
 * (час передається ззовні), тож перевіряється юніт-тестом.
 *
 * Ключ відліку — назва пакета (режим «для кожного окремо») або одне спільне значення (режим «разом для всіх»): у спільному
 * режимі перехід зі стрічки в стрічку відлік не перериває. Перерва довша за [breakMillis] між виходом і поверненням
 * обнуляє відлік цього ключа.
 */
class BerserkTracker(private val breakMillis: Long) {
    private var activeKey: String? = null
    private var activeSince = 0L
    private val accumulated = HashMap<String, Long>()
    private val endedAt = HashMap<String, Long>()

    /** Людина зайшла в стрічку з ключем [key]. Повторний виклик для вже активного ключа нічого не змінює. */
    fun enter(key: String, now: Long) {
        if (activeKey == key) return
        leave(now)
        val ended = endedAt[key]
        if (ended != null && now - ended > breakMillis) accumulated[key] = 0L
        activeKey = key
        activeSince = now
    }

    /** Людина вийшла зі стрічок (робочий стіл, інший застосунок, екран вимкнено). */
    fun leave(now: Long) {
        val key = activeKey ?: return
        accumulated[key] = (accumulated[key] ?: 0L) + (now - activeSince)
        endedAt[key] = now
        activeKey = null
    }

    fun isActive(key: String): Boolean = activeKey == key

    /** Накопичений безперервний час ключа, включно з поточним відрізком. */
    fun elapsed(key: String, now: Long): Long = (accumulated[key] ?: 0L) + if (activeKey == key) now - activeSince else 0L

    /** Відлік ключа починається з нуля (після блоку). */
    fun reset(key: String, now: Long) {
        accumulated.remove(key)
        endedAt.remove(key)
        if (activeKey == key) activeSince = now
    }

    fun clear() {
        activeKey = null
        accumulated.clear()
        endedAt.clear()
    }
}
