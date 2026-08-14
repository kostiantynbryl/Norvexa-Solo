package com.norvexa.flow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.norvexa.flow.data.local.ClientEntity
import com.norvexa.flow.data.local.PlannedExpenseEntity
import com.norvexa.flow.data.local.ReceivableEntity
import com.norvexa.flow.domain.DashboardSummary
import com.norvexa.flow.domain.ReceivableStatus
import com.norvexa.flow.domain.formatMoney
import com.norvexa.flow.ui.components.CashFlowChart
import com.norvexa.flow.ui.components.EmptyState
import com.norvexa.flow.ui.components.GroupCard
import com.norvexa.flow.ui.components.IconBubble
import com.norvexa.flow.ui.components.MetricCard
import com.norvexa.flow.ui.components.ScreenHeader
import com.norvexa.flow.ui.components.SectionHeader
import com.norvexa.flow.ui.components.StatusPill
import java.time.LocalDate

@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    currency: String,
    receivables: List<ReceivableEntity>,
    expenses: List<PlannedExpenseEntity>,
    clients: List<ClientEntity>,
    onPayReceivable: (Long) -> Unit,
    onCompleteExpense: (Long) -> Unit,
) {
    val today = LocalDate.now().toEpochDay()
    val openReceivables = receivables
        .filter { it.status != ReceivableStatus.PAID && it.status != ReceivableStatus.CANCELLED }
        .sortedBy { it.expectedAtEpochDay }
    val upcomingExpenses = expenses.filter { !it.isCompleted }.sortedBy { it.dueAtEpochDay }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = "Обзор",
                subtitle = "Ваши деньги сегодня и ближайшие обязательства",
            )
        }

        item {
            MetricCard(
                title = "Доступно сейчас",
                value = formatMoney(summary.availableNowMinor, currency),
                supporting = "После защищённых резервов, недостающего налогового резерва и обязательных расходов на 30 дней",
                emphasized = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricCard(
                    title = "Баланс",
                    value = formatMoney(summary.totalBalanceMinor, currency),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Ожидается",
                    value = formatMoney(summary.openReceivablesMinor, currency),
                    supporting = if (summary.overdueReceivablesMinor > 0) {
                        "Просрочено ${formatMoney(summary.overdueReceivablesMinor, currency)}"
                    } else {
                        "Без просрочек"
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricCard(
                    title = "Защищено",
                    value = formatMoney(summary.protectedReservesMinor, currency),
                    supporting = "Не считается свободными деньгами",
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Налоговый резерв",
                    value = formatMoney(summary.suggestedTaxReserveMinor, currency),
                    supporting = "План на доходы текущего месяца",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            GroupCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    val healthy = summary.cashGap == null
                    IconBubble(
                        icon = if (healthy) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                        tint = if (healthy) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        containerColor = if (healthy) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        },
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = if (healthy) "Прогноз стабильный" else "Нужен контроль",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            StatusPill(
                                text = if (healthy) "30 дней" else "Риск",
                                positive = healthy,
                            )
                        }
                        Text(
                            text = if (healthy) {
                                "На горизонте 30 дней баланс остаётся выше минимально допустимого уровня с учётом резервов."
                            } else {
                                "${summary.cashGap?.date}: прогноз ${formatMoney(summary.cashGap?.balanceMinor ?: 0, currency)} при минимально допустимом уровне ${formatMoney(summary.cashGap?.safeBalanceMinor ?: 0, currency)}."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { SectionHeader("Прогноз") }

        item {
            GroupCard(Modifier.fillMaxWidth()) {
                CashFlowChart(
                    points = summary.forecast,
                    currency = currency,
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricCard(
                    title = "Через 7 дней",
                    value = formatMoney(summary.projected7Minor, currency),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Через 30 дней",
                    value = formatMoney(summary.projected30Minor, currency),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Spacer(Modifier.height(2.dp))
            SectionHeader("Ближайшие оплаты")
        }

        if (openReceivables.isEmpty()) {
            item {
                EmptyState(
                    title = "Нет ожидаемых оплат",
                    description = "Добавьте клиента и ожидаемую оплату через кнопку +",
                )
            }
        } else {
            item {
                GroupCard(Modifier.fillMaxWidth()) {
                    Column {
                        openReceivables.take(5).forEachIndexed { index, receivable ->
                            val remaining = (receivable.amountMinor - receivable.receivedMinor).coerceAtLeast(0)
                            val overdue = receivable.expectedAtEpochDay < today
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                IconBubble(
                                    icon = Icons.Rounded.Schedule,
                                    tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    containerColor = if (overdue) {
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    },
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        receivable.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "${clients.firstOrNull { it.id == receivable.clientId }?.name ?: "Клиент"} · ${LocalDate.ofEpochDay(receivable.expectedAtEpochDay)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (overdue) {
                                        Text(
                                            "Просрочено",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        formatMoney(remaining, receivable.currency),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    TextButton(
                                        onClick = { onPayReceivable(receivable.id) },
                                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                                    ) { Text("Получено") }
                                }
                            }
                            if (index < openReceivables.take(5).lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 66.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(2.dp))
            SectionHeader("Ближайшие расходы")
        }

        if (upcomingExpenses.isEmpty()) {
            item {
                EmptyState(
                    title = "Нет будущих расходов",
                    description = "Добавьте аренду, подписки и другие обязательства",
                )
            }
        } else {
            item {
                GroupCard(Modifier.fillMaxWidth()) {
                    Column {
                        upcomingExpenses.take(5).forEachIndexed { index, expense ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                IconBubble(
                                    icon = Icons.Rounded.Schedule,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        expense.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "${expense.category} · ${LocalDate.ofEpochDay(expense.dueAtEpochDay)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Column {
                                    Text(
                                        formatMoney(expense.amountMinor, expense.currency),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    TextButton(
                                        onClick = { onCompleteExpense(expense.id) },
                                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                                    ) { Text("Оплачено") }
                                }
                            }
                            if (index < upcomingExpenses.take(5).lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 66.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
