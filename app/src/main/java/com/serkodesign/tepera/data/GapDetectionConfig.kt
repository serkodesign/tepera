package com.serkodesign.tepera.data

/**
 * T-11 (tepera-dev-spec.md): усі порогові значення детекції пауз (FR-D.1) в одному місці, не
 * розкидані константи. Три пресети замість числових полів у Налаштуваннях — числа тут вимагали б
 * від людини розуміння алгоритму (документ, розділ T-11, пункт 2).
 */
enum class GapSensitivity { RARE, NORMAL, FREQUENT }

/**
 * [minGapMinutes] — SRS FR-D.1 (30 хв для [GapSensitivity.NORMAL], чинне значення, не вигадане).
 * [minIntervalBetweenGapsMinutes] — якщо коротка сесія використання розділяє дві паузи-кандидати
 * на менше за цей час, вони зливаються в одну (людина на секунду глянула в телефон посеред довгої
 * відсутності — це не "повернення до використання", а шум, інакше довга дорога на роботу з одним
 * поглядом на годинник посередині стала б двома картками замість однієї).
 * [maxGapsPerDay] — стеля; при перевищенні лишаються найдовші (документ: орієнтир 3-4 для
 * NORMAL — точне значення мало б підтвердити дослідження T-1, який ще не виконаний, тож тут
 * узято середину орієнтиру як робоче рішення).
 */
data class GapDetectionConfig(
    val sensitivity: GapSensitivity,
    val minGapMinutes: Int,
    val minIntervalBetweenGapsMinutes: Int,
    val maxGapsPerDay: Int
) {
    companion object {
        fun forSensitivity(sensitivity: GapSensitivity): GapDetectionConfig = when (sensitivity) {
            GapSensitivity.RARE -> GapDetectionConfig(
                sensitivity = GapSensitivity.RARE,
                minGapMinutes = 60,
                minIntervalBetweenGapsMinutes = 120,
                maxGapsPerDay = 3
            )
            GapSensitivity.NORMAL -> GapDetectionConfig(
                sensitivity = GapSensitivity.NORMAL,
                minGapMinutes = 30, // FR-D.1
                minIntervalBetweenGapsMinutes = 60,
                maxGapsPerDay = 4
            )
            GapSensitivity.FREQUENT -> GapDetectionConfig(
                sensitivity = GapSensitivity.FREQUENT,
                minGapMinutes = 20,
                minIntervalBetweenGapsMinutes = 30,
                maxGapsPerDay = 6
            )
        }
    }
}
