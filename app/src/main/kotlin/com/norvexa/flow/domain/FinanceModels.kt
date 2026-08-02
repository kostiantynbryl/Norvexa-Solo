package com.norvexa.flow.domain

import com.norvexa.flow.data.local.ClientEntity
import com.norvexa.flow.data.local.PlannedExpenseEntity
import com.norvexa.flow.data.local.ReceivableEntity
import com.norvexa.flow.data.local.ReserveEntity
import com.norvexa.flow.data.local.TransactionEntity
import com.norvexa.flow.data.local.WalletEntity
import java.time.LocalDate

object TransactionType { const val INCOME = "INCOME"; const val EXPENSE = "EXPENSE" }
object ReceivableStatus { const val EXPECTED = "EXPECTED"; const val PARTIAL = "PARTIAL"; const val PAID = "PAID"; const val CANCELLED = "CANCELLED" }

data class FinanceData(
    val wallets: List<WalletEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val clients: List<ClientEntity> = emptyList(),
    val receivables: List<ReceivableEntity> = emptyList(),
    val plannedExpenses: List<PlannedExpenseEntity> = emptyList(),
    val reserves: List<ReserveEntity> = emptyList(),
)
data class CashFlowPoint(val date: LocalDate, val balanceMinor: Long)
data class CashGap(val date: LocalDate, val balanceMinor: Long, val safeBalanceMinor: Long)
data class DashboardSummary(
    val totalBalanceMinor: Long = 0,
    val protectedReservesMinor: Long = 0,
    val mandatoryExpenses30Minor: Long = 0,
    val availableNowMinor: Long = 0,
    val openReceivablesMinor: Long = 0,
    val overdueReceivablesMinor: Long = 0,
    val projected7Minor: Long = 0,
    val projected30Minor: Long = 0,
    val suggestedTaxReserveMinor: Long = 0,
    val cashGap: CashGap? = null,
    val forecast: List<CashFlowPoint> = emptyList(),
)
data class PriceInput(
    val desiredNetMonthlyMinor: Long,
    val monthlyBusinessCostsMinor: Long,
    val monthlyReserveContributionMinor: Long,
    val billableHoursPerMonth: Int,
    val projectHours: Int,
    val directProjectCostsMinor: Long,
    val taxPercent: Int,
    val feePercent: Int,
    val riskPercent: Int,
)
data class PriceResult(val requiredMonthlyRevenueMinor: Long, val minimumHourlyRateMinor: Long, val minimumProjectPriceMinor: Long, val recommendedProjectPriceMinor: Long)
data class MarginInput(val revenueMinor: Long, val directCostsMinor: Long, val hours: Int, val hourlyCostMinor: Long, val taxPercent: Int, val feePercent: Int)
data class MarginResult(val cashProfitMinor: Long, val economicProfitMinor: Long, val marginPercent: Int)
