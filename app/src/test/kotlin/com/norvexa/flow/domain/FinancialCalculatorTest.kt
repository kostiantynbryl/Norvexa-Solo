package com.norvexa.flow.domain

import com.norvexa.flow.data.local.PlannedExpenseEntity
import com.norvexa.flow.data.local.ReceivableEntity
import com.norvexa.flow.data.local.ReserveEntity
import com.norvexa.flow.data.local.TransactionEntity
import com.norvexa.flow.data.local.WalletEntity
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FinancialCalculatorTest {
    @Test
    fun dashboardSubtractsProtectedMoneyAndMandatoryExpenses() {
        val today = LocalDate.of(2026, 8, 2)
        val summary = FinancialCalculator.dashboard(
            wallets = listOf(WalletEntity(name = "Main", currency = "USD", balanceMinor = 100_000)),
            transactions = emptyList(),
            receivables = emptyList(),
            expenses = listOf(
                PlannedExpenseEntity(
                    title = "Hosting",
                    amountMinor = 10_000,
                    currency = "USD",
                    dueAtEpochDay = today.plusDays(5).toEpochDay(),
                    category = "Hosting",
                ),
            ),
            reserves = listOf(
                ReserveEntity(
                    name = "Reserve",
                    targetMinor = 30_000,
                    currentMinor = 20_000,
                    currency = "USD",
                ),
            ),
            taxPercent = 10,
            safeBalanceMinor = 5_000,
            today = today,
        )
        assertEquals(70_000, summary.availableNowMinor)
    }

    @Test
    fun taxReserveShortfallReducesAvailableMoney() {
        val today = LocalDate.of(2026, 8, 14)
        val incomeTime = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val summary = FinancialCalculator.dashboard(
            wallets = listOf(WalletEntity(name = "Main", currency = "USD", balanceMinor = 100_000)),
            transactions = listOf(
                TransactionEntity(
                    walletId = 1,
                    type = TransactionType.INCOME,
                    amountMinor = 100_000,
                    currency = "USD",
                    category = "Client",
                    occurredAtEpochMillis = incomeTime,
                ),
            ),
            receivables = emptyList(),
            expenses = emptyList(),
            reserves = emptyList(),
            taxPercent = 10,
            safeBalanceMinor = 0,
            today = today,
        )
        assertEquals(10_000, summary.suggestedTaxReserveMinor)
        assertEquals(90_000, summary.availableNowMinor)
    }

    @Test
    fun forecastUsesPaymentProbability() {
        val today = LocalDate.of(2026, 8, 2)
        val points = FinancialCalculator.buildForecast(
            openingBalanceMinor = 10_000,
            receivables = listOf(
                ReceivableEntity(
                    clientId = 1,
                    title = "Project",
                    amountMinor = 100_000,
                    currency = "USD",
                    expectedAtEpochDay = today.plusDays(1).toEpochDay(),
                    probabilityPercent = 50,
                ),
            ),
            expenses = emptyList(),
            today = today,
            horizonDays = 2,
        )
        assertEquals(60_000, points[1].balanceMinor)
    }

    @Test
    fun overdueExpenseIsAppliedToday() {
        val today = LocalDate.of(2026, 8, 14)
        val points = FinancialCalculator.buildForecast(
            openingBalanceMinor = 100_000,
            receivables = emptyList(),
            expenses = listOf(
                PlannedExpenseEntity(
                    title = "Rent",
                    amountMinor = 10_000,
                    currency = "USD",
                    dueAtEpochDay = today.minusDays(1).toEpochDay(),
                    category = "Rent",
                ),
            ),
            today = today,
            horizonDays = 1,
        )
        assertEquals(90_000, points[0].balanceMinor)
    }

    @Test
    fun monthlyExpenseRepeatsInsideForecast() {
        val today = LocalDate.of(2026, 1, 1)
        val points = FinancialCalculator.buildForecast(
            openingBalanceMinor = 100_000,
            receivables = emptyList(),
            expenses = listOf(
                PlannedExpenseEntity(
                    title = "Subscription",
                    amountMinor = 10_000,
                    currency = "USD",
                    dueAtEpochDay = today.toEpochDay(),
                    category = "Software",
                    recurrence = "MONTHLY",
                ),
            ),
            today = today,
            horizonDays = 31,
        )
        assertEquals(80_000, points.last().balanceMinor)
    }

    @Test
    fun dashboardDetectsCashGap() {
        val today = LocalDate.of(2026, 8, 2)
        val summary = FinancialCalculator.dashboard(
            wallets = listOf(WalletEntity(name = "Main", currency = "USD", balanceMinor = 20_000)),
            transactions = emptyList(),
            receivables = emptyList(),
            expenses = listOf(
                PlannedExpenseEntity(
                    title = "Rent",
                    amountMinor = 18_000,
                    currency = "USD",
                    dueAtEpochDay = today.plusDays(1).toEpochDay(),
                    category = "Rent",
                ),
            ),
            reserves = emptyList(),
            taxPercent = 0,
            safeBalanceMinor = 5_000,
            today = today,
        )
        assertNotNull(summary.cashGap)
        assertEquals(today.plusDays(1), summary.cashGap?.date)
    }

    @Test
    fun convertsZeroFractionCurrencyToBaseCurrency() {
        val usdMinor = FinancialCalculator.toBaseMinor(
            amountMinor = 1_000,
            rateToBaseMicros = 6_700,
            sourceCurrency = "JPY",
            baseCurrency = "USD",
        )
        assertEquals(670, usdMinor)
    }

    @Test
    fun priceCalculatorReturnsSafeMinimum() {
        val result = FinancialCalculator.calculatePrice(
            PriceInput(300_000, 30_000, 30_000, 100, 20, 10_000, 10, 3, 7),
        )
        assertEquals(450_000, result.requiredMonthlyRevenueMinor)
        assertEquals(4_500, result.minimumHourlyRateMinor)
        assertEquals(100_000, result.minimumProjectPriceMinor)
    }

    @Test
    fun marginIncludesTimeCost() {
        val result = FinancialCalculator.calculateMargin(
            MarginInput(100_000, 10_000, 20, 2_000, 10, 3),
        )
        assertEquals(77_000, result.cashProfitMinor)
        assertEquals(37_000, result.economicProfitMinor)
        assertEquals(37, result.marginPercent)
    }
}
