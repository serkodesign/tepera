package com.serkodesign.tepera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * v3.0 MVP, Етап 1 (`docs/CLAUDE-CODE-PLAN-v3-MVP.md`): застосунок-стрічка, який людина САМА підтвердила для
 * підрахунку годин ризику. Наявність рядка = застосунок враховується; автоматично нічого не вмикається, месенджери
 * за замовчуванням не пропонуються. [label] — назва на момент підтвердження (щоб показувати без доступу до
 * PackageManager, напр. у звіті).
 */
@Entity(tableName = "feed_apps")
data class FeedAppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val confirmedAt: Long
)
