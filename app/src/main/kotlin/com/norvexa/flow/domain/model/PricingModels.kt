package com.norvexa.flow.domain.model

import java.math.BigDecimal

data class PricingInput(
    val desiredNetMonthlyMinor: Long,
    val monthlyBusinessCostsMinor: Long,
    val monthlyReserveContributionMinor: Long,
    val billableHoursPerMonth: BigDecimal,
    val taxRate: BigDecimal,
    val paymentFeeRate: BigDecimal,
    val riskBufferRate: BigDecimal,
    val projectHours: BigDecimal,
    val directProjectCostsMinor: Long,
    val projectRiskMultiplier: BigDecimal = BigDecimal.ONE,
)

data class PricingResult(
    val requiredMonthlyRevenueMinor: Long,
    val minimumHourlyRateMinor: Long,
    val minimumProjectPriceMinor: Long,
)

data class MarginInput(
    val revenueMinor: Long,
    val directCostsMinor: Long,
    val paymentFeesMinor: Long,
    val taxReserveMinor: Long,
    val laborCostMinor: Long,
)

data class MarginResult(
    val cashProfitMinor: Long,
    val economicProfitMinor: Long,
    val economicMarginPercent: BigDecimal,
)
