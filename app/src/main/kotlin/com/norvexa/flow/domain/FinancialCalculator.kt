package com.norvexa.flow.domain

import com.norvexa.flow.data.local.PlannedExpenseEntity
import com.norvexa.flow.data.local.ReceivableEntity
import com.norvexa.flow.data.local.ReserveEntity
import com.norvexa.flow.data.local.TransactionEntity
import com.norvexa.flow.data.local.WalletEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object FinancialCalculator {
    private val micros = BigDecimal.valueOf(1_000_000L)
    private val hundred = BigDecimal.valueOf(100L)

    /** Legacy helper for currencies that both use two fraction digits. */
    fun toBaseMinor(amountMinor: Long, rateToBaseMicros: Long): Long =
        BigDecimal.valueOf(amountMinor)
            .multiply(BigDecimal.valueOf(rateToBaseMicros))
            .divide(micros, 0, RoundingMode.HALF_UP)
            .longValueExact()

    fun toBaseMinor(
        amountMinor: Long,
        rateToBaseMicros: Long,
        sourceCurrency: String,
        baseCurrency: String,
    ): Long {
        require(rateToBaseMicros > 0) { "Exchange rate must be positive" }
        val sourceUnits = BigDecimal.valueOf(amountMinor, currencyFractionDigits(sourceCurrency))
        val baseUnits = sourceUnits
            .multiply(BigDecimal.valueOf(rateToBaseMicros))
            .divide(micros, 12, RoundingMode.HALF_UP)
        return baseUnits
            .movePointRight(currencyFractionDigits(baseCurrency))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun fromBaseMinor(
        baseMinor: Long,
        rateToBaseMicros: Long,
        targetCurrency: String,
        baseCurrency: String,
    ): Long {
        require(rateToBaseMicros > 0) { "Exchange rate must be positive" }
        val baseUnits = BigDecimal.valueOf(baseMinor, currencyFractionDigits(baseCurrency))
        val targetUnits = baseUnits
            .multiply(micros)
            .divide(BigDecimal.valueOf(rateToBaseMicros), 12, RoundingMode.HALF_UP)
        return targetUnits
            .movePointRight(currencyFractionDigits(targetCurrency))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun dashboard(
        wallets: List<WalletEntity>,
        transactions: List<TransactionEntity>,
        receivables: List<ReceivableEntity>,
        expenses: List<PlannedExpenseEntity>,
        reserves: List<ReserveEntity>,
        taxPercent: Int,
        safeBalanceMinor: Long,
        today: LocalDate = LocalDate.now(),
        horizonDays: Long = 30,
        baseCurrency: String = "USD",
    ): DashboardSummary {
        val totalBalance = wallets
            .filter { it.isActive }
            .sumOf { toBaseMinor(it.balanceMinor, it.rateToBaseMicros, it.currency, baseCurrency) }
        val protected = reserves
            .filter { it.isProtected }
            .sumOf { toBaseMinor(it.currentMinor, it.rateToBaseMicros, it.currency, baseCurrency) }
        val protectedTax = reserves
            .filter { it.isProtected && it.type == "TAX" }
            .sumOf { toBaseMinor(it.currentMinor, it.rateToBaseMicros, it.currency, baseCurrency) }

        val limit = today.plusDays(horizonDays)
        val mandatory30 = expenses
            .filter { !it.isCompleted && it.isMandatory }
            .sumOf { expense ->
                expenseOccurrenceDates(expense, today, limit).size.toLong() *
                    toBaseMinor(expense.amountMinor, expense.rateToBaseMicros, expense.currency, baseCurrency)
            }

        val open = receivables
            .filter { it.status != ReceivableStatus.PAID && it.status != ReceivableStatus.CANCELLED }
            .sumOf {
                toBaseMinor(
                    (it.amountMinor - it.receivedMinor).coerceAtLeast(0),
                    it.rateToBaseMicros,
                    it.currency,
                    baseCurrency,
                )
            }
        val overdue = receivables
            .filter { it.status != ReceivableStatus.PAID && it.status != ReceivableStatus.CANCELLED }
            .filter { LocalDate.ofEpochDay(it.expectedAtEpochDay).isBefore(today) }
            .sumOf {
                toBaseMinor(
                    (it.amountMinor - it.receivedMinor).coerceAtLeast(0),
                    it.rateToBaseMicros,
                    it.currency,
                    baseCurrency,
                )
            }

        val zone = ZoneId.systemDefault()
        val monthStartMillis = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val nextMonthMillis = today.plusMonths(1).withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthIncome = transactions
            .asSequence()
            .filter { it.type == TransactionType.INCOME }
            .filter { it.occurredAtEpochMillis in monthStartMillis until nextMonthMillis }
            .sumOf { toBaseMinor(it.amountMinor, it.rateToBaseMicros, it.currency, baseCurrency) }
        val suggestedTaxReserve = percentOf(monthIncome, taxPercent)
        val taxReserveShortfall = (suggestedTaxReserve - protectedTax).coerceAtLeast(0)

        val forecast = buildForecast(
            openingBalanceMinor = totalBalance,
            receivables = receivables,
            expenses = expenses,
            today = today,
            horizonDays = horizonDays,
            baseCurrency = baseCurrency,
        )
        val effectiveSafeFloor = safeBalanceMinor + protected + taxReserveShortfall
        val gap = forecast.firstOrNull { it.balanceMinor < effectiveSafeFloor }
            ?.let { CashGap(it.date, it.balanceMinor, effectiveSafeFloor) }

        return DashboardSummary(
            totalBalanceMinor = totalBalance,
            protectedReservesMinor = protected,
            mandatoryExpenses30Minor = mandatory30,
            availableNowMinor = totalBalance - protected - taxReserveShortfall - mandatory30,
            openReceivablesMinor = open,
            overdueReceivablesMinor = overdue,
            projected7Minor = forecast.getOrNull(7)?.balanceMinor ?: totalBalance,
            projected30Minor = forecast.lastOrNull()?.balanceMinor ?: totalBalance,
            suggestedTaxReserveMinor = suggestedTaxReserve,
            cashGap = gap,
            forecast = forecast,
        )
    }

    fun buildForecast(
        openingBalanceMinor: Long,
        receivables: List<ReceivableEntity>,
        expenses: List<PlannedExpenseEntity>,
        today: LocalDate = LocalDate.now(),
        horizonDays: Long = 30,
        baseCurrency: String = "USD",
    ): List<CashFlowPoint> {
        val limit = today.plusDays(horizonDays)
        val expenseByDate = expenses
            .asSequence()
            .filter { !it.isCompleted }
            .flatMap { expense -> expenseOccurrenceDates(expense, today, limit).asSequence().map { it to expense } }
            .groupBy({ it.first }, { it.second })

        var balance = openingBalanceMinor
        return (0L..horizonDays).map { offset ->
            val date = today.plusDays(offset)
            val income = receivables
                .filter { it.status != ReceivableStatus.PAID && it.status != ReceivableStatus.CANCELLED }
                .filter {
                    val expected = LocalDate.ofEpochDay(it.expectedAtEpochDay)
                    if (offset == 0L) !expected.isAfter(today) else expected == date
                }
                .sumOf {
                    percentOf(
                        toBaseMinor(
                            (it.amountMinor - it.receivedMinor).coerceAtLeast(0),
                            it.rateToBaseMicros,
                            it.currency,
                            baseCurrency,
                        ),
                        it.probabilityPercent,
                    )
                }
            val outgoing = expenseByDate[date].orEmpty().sumOf {
                toBaseMinor(it.amountMinor, it.rateToBaseMicros, it.currency, baseCurrency)
            }
            balance += income - outgoing
            CashFlowPoint(date, balance)
        }
    }

    private fun expenseOccurrenceDates(
        expense: PlannedExpenseEntity,
        today: LocalDate,
        limit: LocalDate,
    ): List<LocalDate> {
        val original = LocalDate.ofEpochDay(expense.dueAtEpochDay)
        if (original.isAfter(limit)) return emptyList()
        if (expense.recurrence == "NONE") {
            return listOf(if (original.isBefore(today)) today else original)
        }

        val result = mutableListOf<LocalDate>()
        var occurrence = original
        var guard = 0
        while (!occurrence.isAfter(limit) && guard++ < 1_200) {
            result += if (occurrence.isBefore(today)) today else occurrence
            occurrence = when (expense.recurrence) {
                "MONTHLY" -> occurrence.plusMonths(1)
                "YEARLY" -> occurrence.plusYears(1)
                else -> return listOf(if (original.isBefore(today)) today else original)
            }
        }
        return result
    }

    fun calculatePrice(input: PriceInput): PriceResult {
        require(input.billableHoursPerMonth > 0 && input.projectHours > 0)
        val deductions = input.taxPercent + input.feePercent + input.riskPercent
        require(deductions in 0..95)
        val baseNeed = input.desiredNetMonthlyMinor +
            input.monthlyBusinessCostsMinor +
            input.monthlyReserveContributionMinor
        require(baseNeed >= 0 && input.directProjectCostsMinor >= 0)
        val keepRatio = BigDecimal.valueOf((100 - deductions).toLong())
            .divide(hundred, 8, RoundingMode.HALF_UP)
        val monthlyRevenue = BigDecimal.valueOf(baseNeed)
            .divide(keepRatio, 0, RoundingMode.CEILING)
            .longValueExact()
        val hourly = BigDecimal.valueOf(monthlyRevenue)
            .divide(BigDecimal.valueOf(input.billableHoursPerMonth.toLong()), 0, RoundingMode.CEILING)
            .longValueExact()
        val minimumProject = Math.addExact(
            Math.multiplyExact(hourly, input.projectHours.toLong()),
            input.directProjectCostsMinor,
        )
        val recommended = BigDecimal.valueOf(minimumProject)
            .multiply(BigDecimal("1.10"))
            .setScale(0, RoundingMode.CEILING)
            .longValueExact()
        return PriceResult(monthlyRevenue, hourly, minimumProject, recommended)
    }

    fun calculateMargin(input: MarginInput): MarginResult {
        require(input.revenueMinor > 0)
        require(input.directCostsMinor >= 0 && input.hours >= 0 && input.hourlyCostMinor >= 0)
        require(input.taxPercent in 0..95 && input.feePercent in 0..95)
        require(input.taxPercent + input.feePercent <= 95)
        val cashProfit = input.revenueMinor -
            input.directCostsMinor -
            percentOf(input.revenueMinor, input.taxPercent) -
            percentOf(input.revenueMinor, input.feePercent)
        val economic = cashProfit - Math.multiplyExact(input.hourlyCostMinor, input.hours.toLong())
        val margin = BigDecimal.valueOf(economic)
            .multiply(hundred)
            .divide(BigDecimal.valueOf(input.revenueMinor), 0, RoundingMode.HALF_UP)
            .toInt()
        return MarginResult(cashProfit, economic, margin)
    }

    fun percentOf(valueMinor: Long, percent: Int): Long {
        require(percent in 0..100)
        return BigDecimal.valueOf(valueMinor)
            .multiply(BigDecimal.valueOf(percent.toLong()))
            .divide(hundred, 0, RoundingMode.HALF_UP)
            .longValueExact()
    }
}
