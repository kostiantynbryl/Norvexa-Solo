package com.norvexa.flow.domain.calculation

import com.norvexa.flow.domain.model.CashFlowEvent
import com.norvexa.flow.domain.model.CashFlowEventType
import com.norvexa.flow.domain.model.CashFlowForecast
import com.norvexa.flow.domain.model.FinanceSnapshot
import com.norvexa.flow.domain.model.ForecastPoint
import com.norvexa.flow.domain.model.MarginInput
import com.norvexa.flow.domain.model.MarginResult
import com.norvexa.flow.domain.model.PricingInput
import com.norvexa.flow.domain.model.PricingResult
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

object FinancialCalculator {

    fun snapshot(
        liquidBalanceMinor: Long,
        taxReserveMinor: Long,
        protectedReserveMinor: Long,
        mandatoryExpensesMinor: Long,
    ): FinanceSnapshot {
        require(taxReserveMinor >= 0)
        require(protectedReserveMinor >= 0)
        require(mandatoryExpensesMinor >= 0)

        val available = liquidBalanceMinor
            .safeSubtract(taxReserveMinor)
            .safeSubtract(protectedReserveMinor)
            .safeSubtract(mandatoryExpensesMinor)

        return FinanceSnapshot(
            liquidBalanceMinor = liquidBalanceMinor,
            taxReserveMinor = taxReserveMinor,
            protectedReserveMinor = protectedReserveMinor,
            mandatoryExpensesMinor = mandatoryExpensesMinor,
            availableNowMinor = available,
        )
    }

    fun forecast(
        startBalanceMinor: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        events: List<CashFlowEvent>,
        safeMinimumMinor: Long = 0L,
        weightInflowsByConfidence: Boolean = true,
    ): CashFlowForecast {
        require(!endDate.isBefore(startDate)) { "End date must not be before start date" }

        val eventsByDate = events.groupBy(CashFlowEvent::date)
        val points = mutableListOf<ForecastPoint>()
        var runningBalance = startBalanceMinor
        var date = startDate

        while (!date.isAfter(endDate)) {
            val dailyDelta = eventsByDate[date].orEmpty().fold(0L) { total, event ->
                val signedAmount = when (event.type) {
                    CashFlowEventType.INFLOW -> {
                        if (weightInflowsByConfidence) {
                            weightedMinor(event.amountMinor, event.confidencePercent)
                        } else {
                            event.amountMinor
                        }
                    }

                    CashFlowEventType.OUTFLOW -> -event.amountMinor
                }
                Math.addExact(total, signedAmount)
            }

            runningBalance = Math.addExact(runningBalance, dailyDelta)
            points += ForecastPoint(date = date, balanceMinor = runningBalance)
            date = date.plusDays(1)
        }

        val minimumPoint = points.minBy(ForecastPoint::balanceMinor)
        val firstUnsafeDate = points.firstOrNull { it.balanceMinor < safeMinimumMinor }?.date

        return CashFlowForecast(
            points = points,
            firstUnsafeDate = firstUnsafeDate,
            minimumBalanceMinor = minimumPoint.balanceMinor,
            minimumBalanceDate = minimumPoint.date,
        )
    }

    fun pricing(input: PricingInput): PricingResult {
        require(input.billableHoursPerMonth > BigDecimal.ZERO)
        require(input.projectHours >= BigDecimal.ZERO)
        require(input.projectRiskMultiplier >= BigDecimal.ONE)
        validateRate(input.taxRate)
        validateRate(input.paymentFeeRate)
        validateRate(input.riskBufferRate)

        val retainedShare = BigDecimal.ONE
            .subtract(input.taxRate)
            .subtract(input.paymentFeeRate)
            .subtract(input.riskBufferRate)

        require(retainedShare > BigDecimal.ZERO) {
            "Combined rates must remain below 100%"
        }

        val monthlyNeed = BigDecimal.valueOf(input.desiredNetMonthlyMinor)
            .add(BigDecimal.valueOf(input.monthlyBusinessCostsMinor))
            .add(BigDecimal.valueOf(input.monthlyReserveContributionMinor))

        val requiredRevenue = monthlyNeed.divide(retainedShare, 12, RoundingMode.CEILING)
        val hourlyRate = requiredRevenue.divide(
            input.billableHoursPerMonth,
            12,
            RoundingMode.CEILING,
        )
        val projectPrice = hourlyRate
            .multiply(input.projectHours)
            .add(BigDecimal.valueOf(input.directProjectCostsMinor))
            .multiply(input.projectRiskMultiplier)

        return PricingResult(
            requiredMonthlyRevenueMinor = requiredRevenue.toMinorCeiling(),
            minimumHourlyRateMinor = hourlyRate.toMinorCeiling(),
            minimumProjectPriceMinor = projectPrice.toMinorCeiling(),
        )
    }

    fun margin(input: MarginInput): MarginResult {
        val cashProfit = input.revenueMinor
            .safeSubtract(input.directCostsMinor)
            .safeSubtract(input.paymentFeesMinor)
            .safeSubtract(input.taxReserveMinor)

        val economicProfit = cashProfit.safeSubtract(input.laborCostMinor)
        val marginPercent = if (input.revenueMinor == 0L) {
            BigDecimal.ZERO
        } else {
            BigDecimal.valueOf(economicProfit)
                .divide(BigDecimal.valueOf(input.revenueMinor), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100L))
                .setScale(2, RoundingMode.HALF_UP)
        }

        return MarginResult(
            cashProfitMinor = cashProfit,
            economicProfitMinor = economicProfit,
            economicMarginPercent = marginPercent,
        )
    }

    private fun validateRate(rate: BigDecimal) {
        require(rate >= BigDecimal.ZERO && rate < BigDecimal.ONE) {
            "Rate must be between 0 (inclusive) and 1 (exclusive)"
        }
    }

    private fun weightedMinor(amountMinor: Long, confidencePercent: Int): Long =
        BigDecimal.valueOf(amountMinor)
            .multiply(BigDecimal.valueOf(confidencePercent.toLong()))
            .divide(BigDecimal.valueOf(100L), 0, RoundingMode.HALF_UP)
            .longValueExact()

    private fun BigDecimal.toMinorCeiling(): Long =
        setScale(0, RoundingMode.CEILING).longValueExact()

    private fun Long.safeSubtract(value: Long): Long = Math.subtractExact(this, value)
}
