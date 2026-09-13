package com.serkodesign.tepera.data.local.entity

/**
 * T-3/T-10/T-14 (tepera-dev-spec.md): яку "оцінка → реальність" метрику стосується
 * [UserEstimateEntity]. ONLINE_HOURS — онбординг (T-3, зроблено); LAST_PHONE_USE/UNLOCK_COUNT —
 * заплановані окремими сесіями (T-10/T-14), той самий стіл, інший [EstimateType].
 */
enum class EstimateType { ONLINE_HOURS, LAST_PHONE_USE, UNLOCK_COUNT }
