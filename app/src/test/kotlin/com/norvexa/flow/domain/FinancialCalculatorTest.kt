package com.norvexa.flow.domain

import com.norvexa.flow.data.local.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class FinancialCalculatorTest {
    @Test fun dashboardSubtractsProtectedMoneyAndMandatoryExpenses(){val today=LocalDate.of(2026,8,2);val s=FinancialCalculator.dashboard(listOf(WalletEntity(name="Main",currency="USD",balanceMinor=100_000)),emptyList(),emptyList(),listOf(PlannedExpenseEntity(title="Hosting",amountMinor=10_000,currency="USD",dueAtEpochDay=today.plusDays(5).toEpochDay(),category="Hosting")),listOf(ReserveEntity(name="Tax",targetMinor=30_000,currentMinor=20_000,currency="USD")),10,5_000,today);assertEquals(70_000,s.availableNowMinor)}
    @Test fun forecastUsesPaymentProbability(){val today=LocalDate.of(2026,8,2);val p=FinancialCalculator.buildForecast(10_000,listOf(ReceivableEntity(clientId=1,title="Project",amountMinor=100_000,currency="USD",expectedAtEpochDay=today.plusDays(1).toEpochDay(),probabilityPercent=50)),emptyList(),today,2);assertEquals(60_000,p[1].balanceMinor)}
    @Test fun dashboardDetectsCashGap(){val today=LocalDate.of(2026,8,2);val s=FinancialCalculator.dashboard(listOf(WalletEntity(name="Main",currency="USD",balanceMinor=20_000)),emptyList(),emptyList(),listOf(PlannedExpenseEntity(title="Rent",amountMinor=18_000,currency="USD",dueAtEpochDay=today.plusDays(1).toEpochDay(),category="Rent")),emptyList(),0,5_000,today);assertNotNull(s.cashGap);assertEquals(today.plusDays(1),s.cashGap?.date)}
    @Test fun priceCalculatorReturnsSafeMinimum(){val r=FinancialCalculator.calculatePrice(PriceInput(300_000,30_000,30_000,100,20,10_000,10,3,7));assertEquals(450_000,r.requiredMonthlyRevenueMinor);assertEquals(4_500,r.minimumHourlyRateMinor);assertEquals(100_000,r.minimumProjectPriceMinor)}
    @Test fun marginIncludesTimeCost(){val r=FinancialCalculator.calculateMargin(MarginInput(100_000,10_000,20,2_000,10,3));assertEquals(77_000,r.cashProfitMinor);assertEquals(37_000,r.economicProfitMinor);assertEquals(37,r.marginPercent)}
}
