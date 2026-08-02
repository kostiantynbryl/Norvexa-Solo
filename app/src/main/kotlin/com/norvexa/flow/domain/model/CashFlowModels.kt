package com.norvexa.flow.domain.model

import java.time.LocalDate

enum class CashFlowEventType {
    INFLOW,
    OUTFLOW,
}

data class CashFlowEvent(
    val id: String,
    val title: String,
    val date: LocalDate,
    val amountMinor: Long,
    val type: CashFlowEventType,
    val confidencePercent: Int = 100,
) {
    init {
        require(amountMinor >= 0) { "Event amount cannot be negative" }
        require(confidencePercent in 0..100) { "Confidence must be between 0 and 100" }
    }
}

data class ForecastPoint(
    val date: LocalDate,
    val balanceMinor: Long,
)

data class CashFlowForecast(
    val points: List<ForecastPoint>,
    val firstUnsafeDate: LocalDate?,
    val minimumBalanceMinor: Long,
    val minimumBalanceDate: LocalDate,
)

data class FinanceSnapshot(
    val liquidBalanceMinor: Long,
    val taxReserveMinor: Long,
    val protectedReserveMinor: Long,
    val mandatoryExpensesMinor: Long,
    val availableNowMinor: Long,
)
