package com.serkodesign.tepera.data.repository

import com.serkodesign.tepera.data.local.dao.UserEstimateDao
import com.serkodesign.tepera.data.local.entity.EstimateType
import com.serkodesign.tepera.data.local.entity.UserEstimateEntity
import java.time.LocalDate

/**
 * T-3/T-10/T-14 (tepera-dev-spec.md): CRUD над `user_estimates` — ViewModel ніколи не торкається
 * UserEstimateDao напряму (CLAUDE.md, "Архітектура").
 */
class UserEstimateRepository(private val dao: UserEstimateDao) {

    suspend fun saveEstimate(type: EstimateType, estimatedValue: Long, forDate: LocalDate) {
        dao.insert(UserEstimateEntity(type = type, estimatedValue = estimatedValue, actualValue = null, forDate = forDate))
    }

    /**
     * T-14: на відміну від [saveEstimate] (T-3, де реальне значення з'являється пізніше,
     * можливо, іншого дня), тут доступ до статистики вже гарантовано наданий у момент вибору
     * оцінки — реальне значення рахується одразу, немає сенсу зберігати проміжний "нерозв'язаний"
     * стан.
     */
    suspend fun saveResolvedEstimate(type: EstimateType, estimatedValue: Long, actualValue: Long, forDate: LocalDate) {
        dao.insert(UserEstimateEntity(type = type, estimatedValue = estimatedValue, actualValue = actualValue, forDate = forDate))
    }

    suspend fun getLatestUnresolved(type: EstimateType): UserEstimateEntity? = dao.getLatestUnresolved(type)

    suspend fun setActualValue(id: String, actualValue: Long) = dao.setActualValue(id, actualValue)
}
