package com.norvexa.flow.ui.dashboard

import androidx.lifecycle.ViewModel
import com.norvexa.flow.domain.calculation.FinancialCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private fun createInitialState(): DashboardUiState {
        val snapshot = FinancialCalculator.snapshot(
            liquidBalanceMinor = 0L,
            taxReserveMinor = 0L,
            protectedReserveMinor = 0L,
            mandatoryExpensesMinor = 0L,
        )

        return DashboardUiState(
            currencyCode = "USD",
            availableNowMinor = snapshot.availableNowMinor,
            totalBalanceMinor = snapshot.liquidBalanceMinor,
            reservedMinor = snapshot.taxReserveMinor + snapshot.protectedReserveMinor,
            upcomingExpensesMinor = snapshot.mandatoryExpensesMinor,
            forecast30DaysMinor = snapshot.availableNowMinor,
            firstRiskDate = null,
            outstandingReceivablesMinor = 0L,
            overdueReceivablesMinor = 0L,
            isEmpty = true,
        )
    }
}

data class DashboardUiState(
    val currencyCode: String,
    val availableNowMinor: Long,
    val totalBalanceMinor: Long,
    val reservedMinor: Long,
    val upcomingExpensesMinor: Long,
    val forecast30DaysMinor: Long,
    val firstRiskDate: LocalDate?,
    val outstandingReceivablesMinor: Long,
    val overdueReceivablesMinor: Long,
    val isEmpty: Boolean,
)
