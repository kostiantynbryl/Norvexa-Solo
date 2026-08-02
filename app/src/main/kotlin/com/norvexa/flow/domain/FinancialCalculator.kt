package com.norvexa.flow.domain

import com.norvexa.flow.data.local.PlannedExpenseEntity
import com.norvexa.flow.data.local.ReceivableEntity
import com.norvexa.flow.data.local.ReserveEntity
import com.norvexa.flow.data.local.TransactionEntity
import com.norvexa.flow.data.local.WalletEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

object FinancialCalculator {
    private val micros = BigDecimal.valueOf(1_000_000L)
    private val hundred = BigDecimal.valueOf(100L)

    fun toBaseMinor(amountMinor: Long, rateToBaseMicros: Long): Long = BigDecimal.valueOf(amountMinor).multiply(BigDecimal.valueOf(rateToBaseMicros)).divide(micros, 0, RoundingMode.HALF_UP).longValueExact()

    fun dashboard(
        wallets: List<WalletEntity>, transactions: List<TransactionEntity>, receivables: List<ReceivableEntity>,
        expenses: List<PlannedExpenseEntity>, reserves: List<ReserveEntity>, taxPercent: Int, safeBalanceMinor: Long,
        today: LocalDate = LocalDate.now(), horizonDays: Long = 30,
    ): DashboardSummary {
        val totalBalance = wallets.filter { it.isActive }.sumOf { toBaseMinor(it.balanceMinor, it.rateToBaseMicros) }
        val protected = reserves.filter { it.isProtected }.sumOf { toBaseMinor(it.currentMinor, it.rateToBaseMicros) }
        val limit = today.plusDays(horizonDays)
        val mandatory30 = expenses.filter { !it.isCompleted && it.isMandatory }.filter { LocalDate.ofEpochDay(it.dueAtEpochDay) in today..limit }.sumOf { toBaseMinor(it.amountMinor, it.rateToBaseMicros) }
        val open = receivables.filter { it.status != ReceivableStatus.PAID && it.status != ReceivableStatus.CANCELLED }.sumOf { toBaseMinor((it.amountMinor - it.receivedMinor).coerceAtLeast(0), it.rateToBaseMicros) }
        val overdue = receivables.filter { it.status != ReceivableStatus.PAID && it.status != ReceivableStatus.CANCELLED }.filter { LocalDate.ofEpochDay(it.expectedAtEpochDay).isBefore(today) }.sumOf { toBaseMinor((it.amountMinor - it.receivedMinor).coerceAtLeast(0), it.rateToBaseMicros) }
        val positiveIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { toBaseMinor(it.amountMinor, it.rateToBaseMicros) }
        val forecast = buildForecast(totalBalance, receivables, expenses, today, horizonDays)
        val gap = forecast.firstOrNull { it.balanceMinor < safeBalanceMinor }?.let { CashGap(it.date, it.balanceMinor, safeBalanceMinor) }
        return DashboardSummary(totalBalance, protected, mandatory30, totalBalance - protected - mandatory30, open, overdue, forecast.getOrNull(7)?.balanceMinor ?: totalBalance, forecast.lastOrNull()?.balanceMinor ?: totalBalance, percentOf(positiveIncome, taxPercent), gap, forecast)
    }

    fun buildForecast(openingBalanceMinor: Long, receivables: List<ReceivableEntity>, expenses: List<PlannedExpenseEntity>, today: LocalDate = LocalDate.now(), horizonDays: Long = 30): List<CashFlowPoint> {
        var balance = openingBalanceMinor
        return (0L..horizonDays).map { offset ->
            val date = today.plusDays(offset)
            val income = receivables.filter { it.status != ReceivableStatus.PAID && it.status != ReceivableStatus.CANCELLED && it.expectedAtEpochDay == date.toEpochDay() }.sumOf {
                percentOf(toBaseMinor((it.amountMinor - it.receivedMinor).coerceAtLeast(0), it.rateToBaseMicros), it.probabilityPercent)
            }
            val outgoing = expenses.filter { !it.isCompleted && it.dueAtEpochDay == date.toEpochDay() }.sumOf { toBaseMinor(it.amountMinor, it.rateToBaseMicros) }
            balance += income - outgoing
            CashFlowPoint(date, balance)
        }
    }

    fun calculatePrice(input: PriceInput): PriceResult {
        require(input.billableHoursPerMonth > 0 && input.projectHours > 0)
        val deductions = input.taxPercent + input.feePercent + input.riskPercent
        require(deductions in 0..95)
        val baseNeed = input.desiredNetMonthlyMinor + input.monthlyBusinessCostsMinor + input.monthlyReserveContributionMinor
        val keepRatio = BigDecimal.valueOf((100 - deductions).toLong()).divide(hundred, 8, RoundingMode.HALF_UP)
        val monthlyRevenue = BigDecimal.valueOf(baseNeed).divide(keepRatio, 0, RoundingMode.CEILING).longValueExact()
        val hourly = BigDecimal.valueOf(monthlyRevenue).divide(BigDecimal.valueOf(input.billableHoursPerMonth.toLong()), 0, RoundingMode.CEILING).longValueExact()
        val minimumProject = hourly * input.projectHours + input.directProjectCostsMinor
        val recommended = BigDecimal.valueOf(minimumProject).multiply(BigDecimal("1.10")).setScale(0, RoundingMode.CEILING).longValueExact()
        return PriceResult(monthlyRevenue, hourly, minimumProject, recommended)
    }

    fun calculateMargin(input: MarginInput): MarginResult {
        require(input.revenueMinor > 0)
        val cashProfit = input.revenueMinor - input.directCostsMinor - percentOf(input.revenueMinor, input.taxPercent) - percentOf(input.revenueMinor, input.feePercent)
        val economic = cashProfit - input.hourlyCostMinor * input.hours
        val margin = BigDecimal.valueOf(economic).multiply(hundred).divide(BigDecimal.valueOf(input.revenueMinor), 0, RoundingMode.HALF_UP).toInt()
        return MarginResult(cashProfit, economic, margin)
    }

    fun percentOf(valueMinor: Long, percent: Int): Long = BigDecimal.valueOf(valueMinor).multiply(BigDecimal.valueOf(percent.toLong())).divide(hundred, 0, RoundingMode.HALF_UP).longValueExact()
}
