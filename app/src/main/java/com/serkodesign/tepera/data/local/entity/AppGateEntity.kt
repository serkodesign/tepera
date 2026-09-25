package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * T-4 (tepera-dev-spec.md, FR-G частина 1): ворота — застосунок, що відкривається лише через
 * закріплений ярлик Tepera, з паузою перед фактичним запуском (сам екран паузи — T-5, окрема
 * сесія). Один рядок на застосунок ([packageName] — первинний ключ, не UUID: ворота інгерентно
 * прив'язані до конкретного пакета, другого гейта на той самий застосунок бути не може).
 *
 * [originalIconHandled] — чи підтвердив користувач крок "прибрав оригінальну іконку застосунку
 * з цього лаунчера" (Android не дозволяє зробити це програмно). За замовчуванням false одразу
 * після створення воріт; список воріт (`GatesScreen`) явно показує незавершений стан —
 * найчастіша точка розгубленості за прямою вимогою документа.
 *
 * [lastProceedAtMillis] — T-5 (tepera-dev-spec.md, FR-G частина 2): момент останнього успішного
 * проходження паузи для цього застосунку. `GatePauseViewModel` звіряє з ним "повторний тап
 * протягом 30 с після успішного проходу не показує паузу вдруге" — 0L = ще ніколи не проходив.
 *
 * [shortcutId] — реальний баг, знайдений користувачем на Samsung S23 (версія 9, `MIGRATION_8_9`):
 * ID закріпленого ярлика тепер зберігається в рядку, а не обчислюється наживо з [packageName] —
 * кожне створення воріт генерує СВІЖИЙ унікальний ID, щоб система завжди показувала справжній
 * діалог розміщення на робочому столі. Деталі — `GateRepository.createGate()`.
 */
@Entity(tableName = "app_gates")
data class AppGateEntity(
    @PrimaryKey val packageName: String,
    val delaySeconds: Int,
    val originalIconHandled: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val lastProceedAtMillis: Long = 0L,
    val shortcutId: String = "gate_$packageName",
    val repeatCount: Int = 0,
    val lastShownAtMillis: Long = 0L
)
