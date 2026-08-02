package com.norvexa.flow.domain.calculation

import com.norvexa.flow.domain.model.CashFlowEvent
import com.norvexa.flow.domain.model.CashFlowEventType
import com.norvexa.flow.domain.model.MarginInput
import com.norvexa.flow.domain.model.PricingInput
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinancialCalculatorTest {

    @Test
    fun `available amount excludes reserves and mandatory expenses`() {
        val result = FinancialCalculator.snapshot(
            liquidBalanceMinor = 300_000,
            taxReserveMinor = 40_000,
            protectedReserveMinor = 60_000,
            mandatoryExpensesMinor = 75_000,
        )

        assertEquals(125_000L, result.availableNowMinor)
    }

    @Test
    fun `forecast weights uncertain income and detects cash gap`() {
        val start = LocalDate.of(2026, 8, 1)
        val result = FinancialCalculator.forecast(
            startBalanceMinor = 100_000,
            startDate = start,
            endDate = start.plusDays(5),
            safeMinimumMinor = 20_000,
            events = listOf(
                CashFlowEvent(
                    id = "income",
                    title = "Client payment",
                    date = start.plusDays(1),
                    amountMinor = 100_000,
                    type = CashFlowEventType.INFLOW,
                    confidencePercent = 50,
                ),
                CashFlowEvent(
                    id = "expense",
                    title = "Rent",
                    date = start.plusDays(2),
                    amountMinor = 140_000,
                    type = CashFlowEventType.OUTFLOW,
                ),
            ),
        )

        assertEquals(start.plusDays(2), result.firstUnsafeDate)
        assertEquals(10_000L, result.minimumBalanceMinor)
    }

    @Test
    fun `forecast reports no risk when balance stays safe`() {
        val start = LocalDate.of(2026, 8, 1)
        val result = FinancialCalculator.forecast(
            startBalanceMinor = 100_000,
            startDate = start,
            endDate = start.plusDays(2),
            safeMinimumMinor = 20_000,
            events = emptyList(),
        )

        assertNull(result.firstUnsafeDate)
    }

    @Test
    fun `pricing calculator includes tax fees and risk buffer`() {
        val result = FinancialCalculator.pricing(
            PricingInput(
                desiredNetMonthlyMinor = 200_000,
                monthlyBusinessCostsMinor = 50_000,
                monthlyReserveContributionMinor = 25_000,
                billableHoursPerMonth = BigDecimal("100"),
                taxRate = BigDecimal("0.10"),
                paymentFeeRate = BigDecimal("0.03"),
                riskBufferRate = BigDecimal("0.07"),
                projectHours = BigDecimal("20"),
                directProjectCostsMinor = 10_000,
                projectRiskMultiplier = BigDecimal("1.10"),
            ),
        )

        assertEquals(343_750L, result.requiredMonthlyRevenueMinor)
        assertEquals(3_438L, result.minimumHourlyRateMinor)
        assertEquals(86_625L, result.minimumProjectPriceMinor)
    }

    @Test
    fun `margin separates cash and economic profit`() {
        val result = FinancialCalculator.margin(
            MarginInput(
                revenueMinor = 100_000,
                directCostsMinor = 10_000,
                paymentFeesMinor = 3_000,
                taxReserveMinor = 10_000,
                laborCostMinor = 40_000,
            ),
        )

        assertEquals(77_000L, result.cashProfitMinor)
        assertEquals(37_000L, result.economicProfitMinor)
        assertEquals(BigDecimal("37.00"), result.economicMarginPercent)
    }
}
