package com.norvexa.flow.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.norvexa.flow.ui.theme.NorvexaFlowTheme
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddReceivable: () -> Unit,
    onOpenPricing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Financial overview",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Your current position and the next 30 days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            AvailableNowCard(state = state)
        }

        if (state.isEmpty) {
            item {
                EmptySetupCard(onAddIncome = onAddIncome)
            }
        }

        item {
            ForecastCard(state = state)
        }

        item {
            ReceivablesCard(state = state, onAddReceivable = onAddReceivable)
        }

        item {
            QuickActions(
                onAddIncome = onAddIncome,
                onAddExpense = onAddExpense,
                onAddReceivable = onAddReceivable,
                onOpenPricing = onOpenPricing,
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun AvailableNowCard(state: DashboardUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Available now", style = MaterialTheme.typography.titleMedium)
                Icon(Icons.Outlined.Info, contentDescription = "Explain calculation")
            }
            Text(
                text = formatMoney(state.availableNowMinor, state.currencyCode),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Balance minus protected reserves and mandatory expenses",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Metric(
                    label = "Total balance",
                    value = formatMoney(state.totalBalanceMinor, state.currencyCode),
                    modifier = Modifier.weight(1f),
                )
                Metric(
                    label = "Reserved",
                    value = formatMoney(state.reservedMinor, state.currencyCode),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ForecastCard(state: DashboardUiState) {
    Card {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                Text(
                    text = "30-day forecast",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Text(
                text = formatMoney(state.forecast30DaysMinor, state.currencyCode),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val riskText = state.firstRiskDate?.let {
                "Balance may fall below the safe level on ${it.format(DateTimeFormatter.ofPattern("d MMM"))}."
            } ?: "No cash gap is detected in the current plan."
            Text(
                text = riskText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReceivablesCard(
    state: DashboardUiState,
    onAddReceivable: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Expected client payments", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Metric(
                    label = "Expected",
                    value = formatMoney(state.outstandingReceivablesMinor, state.currencyCode),
                    modifier = Modifier.weight(1f),
                )
                Metric(
                    label = "Overdue",
                    value = formatMoney(state.overdueReceivablesMinor, state.currencyCode),
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedButton(onClick = onAddReceivable) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("Add expected payment", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun EmptySetupCard(onAddIncome: () -> Unit) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
            )
            Text(
                text = "Set up your first balance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Add a wallet, the nearest expected payment, and the next mandatory expense to get a useful forecast.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAddIncome) {
                Text("Start setup")
            }
        }
    }
}

@Composable
private fun QuickActions(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddReceivable: () -> Unit,
    onOpenPricing: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Quick actions", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickAction(
                text = "Income",
                icon = { Icon(Icons.Outlined.ArrowDownward, contentDescription = null) },
                onClick = onAddIncome,
                modifier = Modifier.weight(1f),
            )
            QuickAction(
                text = "Expense",
                icon = { Icon(Icons.Outlined.ArrowUpward, contentDescription = null) },
                onClick = onAddExpense,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickAction(
                text = "Expected payment",
                icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                onClick = onAddReceivable,
                modifier = Modifier.weight(1f),
            )
            QuickAction(
                text = "Price calculator",
                icon = { Icon(Icons.Outlined.Calculate, contentDescription = null) },
                onClick = onOpenPricing,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickAction(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
    ) {
        icon()
        Text(text = text, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun formatMoney(minorUnits: Long, currencyCode: String): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    formatter.currency = Currency.getInstance(currencyCode)
    return formatter.format(minorUnits.toBigDecimal().movePointLeft(2))
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DashboardPreview() {
    NorvexaFlowTheme {
        Box {
            DashboardScreen(
                state = DashboardUiState(
                    currencyCode = "USD",
                    availableNowMinor = 185_000,
                    totalBalanceMinor = 320_000,
                    reservedMinor = 70_000,
                    upcomingExpensesMinor = 65_000,
                    forecast30DaysMinor = 242_000,
                    firstRiskDate = null,
                    outstandingReceivablesMinor = 410_000,
                    overdueReceivablesMinor = 55_000,
                    isEmpty = false,
                ),
                onAddIncome = {},
                onAddExpense = {},
                onAddReceivable = {},
                onOpenPricing = {},
            )
        }
    }
}
