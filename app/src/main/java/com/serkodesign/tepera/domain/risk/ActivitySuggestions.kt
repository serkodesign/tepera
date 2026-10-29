package com.serkodesign.tepera.domain.risk

import kotlin.random.Random

/**
 * v3.0 MVP (beta): підказки «що зробити замість стрічки» замість плану «якщо — то», який людина складала сама.
 * Чиста логіка без Android: обирає ОДНУ пропозицію за порою доби вікна, будень/вихідний, наявними категоріями й
 * недавніми відмовами. Жодних оцінок і лічильників для людини: відмова лише на кілька днів ховає саму пропозицію.
 */
object ActivitySuggestions {
    /** Скільки днів не повторювати пропозицію, від якої людина відмовилась. */
    const val DECLINE_DAYS = 3

    enum class Id { BOOK, PODCAST, TODO_TOMORROW, CHARGE_PHONE, BREATHE, WALK, WATER, STRETCH, EAT_NO_PHONE, CALL_FRIEND, HOBBY }

    /** [categoryId] — якщо пропозиція відповідає категорії застосунку: «Так» тоді запускає її таймер. */
    data class Suggestion(val id: Id, val categoryId: String?)

    private const val NATURE = "default-nature"
    private const val READING = "default-reading"
    private const val HOBBY = "default-hobby"
    private const val MOVEMENT = "default-movement"
    private const val SOCIAL = "default-social"
    private const val ERRANDS = "default-errands"

    private val BOOK = Suggestion(Id.BOOK, READING)
    private val PODCAST = Suggestion(Id.PODCAST, null)
    private val TODO_TOMORROW = Suggestion(Id.TODO_TOMORROW, ERRANDS)
    private val CHARGE_PHONE = Suggestion(Id.CHARGE_PHONE, null)
    private val BREATHE = Suggestion(Id.BREATHE, null)
    private val WALK = Suggestion(Id.WALK, NATURE)
    private val WATER = Suggestion(Id.WATER, null)
    private val STRETCH = Suggestion(Id.STRETCH, MOVEMENT)
    private val EAT_NO_PHONE = Suggestion(Id.EAT_NO_PHONE, null)
    private val CALL_FRIEND = Suggestion(Id.CALL_FRIEND, SOCIAL)
    private val HOBBY_SG = Suggestion(Id.HOBBY, HOBBY)

    /**
     * Що доречно, коли: вечір і ніч — спокійне (читання, подкаст з вимкненим екраном, справи на завтра, телефон
     * на зарядку), без рухового; ранок — рух і вода; обід — прогулянка й їжа без телефона; день — залежить від будня.
     */
    fun pool(part: PartOfDay, weekend: Boolean): List<Suggestion> = when (part) {
        PartOfDay.MORNING -> listOf(WALK, STRETCH, WATER, BREATHE)
        PartOfDay.LUNCH -> listOf(WALK, EAT_NO_PHONE, WATER)
        PartOfDay.DAY ->
            if (weekend) listOf(WALK, HOBBY_SG, CALL_FRIEND, BOOK) else listOf(STRETCH, WATER, BREATHE, CALL_FRIEND)
        PartOfDay.EVENING -> listOf(BOOK, HOBBY_SG, CALL_FRIEND, PODCAST, TODO_TOMORROW)
        PartOfDay.NIGHT -> listOf(BOOK, CHARGE_PHONE, TODO_TOMORROW, BREATHE, PODCAST)
    }

    /**
     * Одна пропозиція або `null`, якщо пулу нема. [availableCategories] — id активних категорій (пропозиція з
     * категорією, якої людина не лишила, не береться). [declined] — id → коли відмовились; [exclude] — вже показані
     * у цьому відкритті («Інша ідея» не повторює щойно показане). Якщо все відфільтровано відмовами — беремо без них,
     * але все одно без [exclude].
     */
    fun pick(
        part: PartOfDay,
        weekend: Boolean,
        availableCategories: Set<String>,
        declined: Map<Id, Long>,
        exclude: Set<Id>,
        nowMillis: Long,
        random: Random = Random.Default
    ): Suggestion? {
        val usable = pool(part, weekend).filter { it.categoryId == null || it.categoryId in availableCategories }
        val fresh = usable.filter { it.id !in exclude }
        val recent = nowMillis - DECLINE_DAYS * 24L * 3_600_000L
        val notDeclined = fresh.filter { (declined[it.id] ?: 0L) < recent }
        return notDeclined.ifEmpty { fresh }.ifEmpty { usable }.let { if (it.isEmpty()) null else it[random.nextInt(it.size)] }
    }
}
