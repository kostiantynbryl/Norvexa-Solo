package com.norvexa.flow.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.norvexa.flow.data.local.ClientEntity
import com.norvexa.flow.data.local.PlannedExpenseEntity
import com.norvexa.flow.data.local.ReceivableEntity
import com.norvexa.flow.data.local.ReserveEntity
import com.norvexa.flow.data.local.TransactionEntity
import com.norvexa.flow.data.local.WalletEntity
import com.norvexa.flow.data.settings.UserSettings
import com.norvexa.flow.domain.ReceivableStatus
import com.norvexa.flow.domain.TransactionType
import com.norvexa.flow.domain.formatMoney
import com.norvexa.flow.ui.components.EmptyState
import com.norvexa.flow.ui.components.GroupCard
import com.norvexa.flow.ui.components.IconBubble
import com.norvexa.flow.ui.components.ScreenHeader
import com.norvexa.flow.ui.components.SectionHeader
import com.norvexa.flow.ui.components.StatusPill
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ActivityScreen(
    transactions: List<TransactionEntity>,
    wallets: List<WalletEntity>,
    clients: List<ClientEntity>,
    baseCurrency: String,
    onDelete: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Операции",
                subtitle = "Фактические движения · базовая валюта $baseCurrency",
            )
        }

        if (wallets.isEmpty()) {
            item {
                EmptyState(
                    title = "Добавьте кошелёк",
                    description = "Без кошелька нельзя сохранить первую операцию",
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                EmptyState(
                    title = "Операций пока нет",
                    description = "Добавьте первый доход или расход через кнопку +",
                )
            }
        }

        items(transactions, key = { it.id }) { transaction ->
            val wallet = wallets.firstOrNull { it.id == transaction.walletId }
            val client = clients.firstOrNull { it.id == transaction.clientId }
            val date = Instant.ofEpochMilli(transaction.occurredAtEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val income = transaction.type == TransactionType.INCOME

            GroupCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconBubble(
                        icon = if (income) Icons.Rounded.SouthWest else Icons.Rounded.NorthEast,
                        tint = if (income) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        containerColor = if (income) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                        },
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            transaction.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            listOfNotNull(
                                wallet?.name,
                                client?.name,
                                date.toString(),
                                transaction.note.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            (if (income) "+ " else "− ") + formatMoney(transaction.amountMinor, transaction.currency),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (income) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(
                            onClick = { onDelete(transaction.id) },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientsScreen(
    clients: List<ClientEntity>,
    receivables: List<ReceivableEntity>,
    onPaid: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val open = receivables.filter {
        it.status != ReceivableStatus.PAID && it.status != ReceivableStatus.CANCELLED
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Клиенты",
                subtitle = "Ожидаемые платежи и задолженности",
            )
        }

        if (clients.isEmpty()) {
            item {
                EmptyState(
                    title = "Клиентов пока нет",
                    description = "Добавьте клиента через кнопку +",
                )
            }
        }

        items(clients, key = { "c${it.id}" }) { client ->
            val debts = open.filter { it.clientId == client.id }
            val total = debts.sumOf { (it.amountMinor - it.receivedMinor).coerceAtLeast(0) }

            GroupCard(Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                client.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (client.email.isNotBlank()) {
                                Text(
                                    client.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatMoney(total, client.defaultCurrency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            StatusPill(
                                text = if (debts.isEmpty()) "Оплачено" else "${debts.size} открыто",
                                positive = debts.isEmpty(),
                            )
                        }
                    }

                    if (debts.isEmpty()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                        Text(
                            "Нет открытых оплат",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        debts.forEach { receivable ->
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(receivable.title, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "До ${LocalDate.ofEpochDay(receivable.expectedAtEpochDay)} · ${receivable.probabilityPercent}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    formatMoney(
                                        (receivable.amountMinor - receivable.receivedMinor).coerceAtLeast(0),
                                        receivable.currency,
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                IconButton(
                                    onClick = { onPaid(receivable.id) },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Done,
                                        contentDescription = "Получено",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(receivable.id) },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Удалить",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanningScreen(
    expenses: List<PlannedExpenseEntity>,
    reserves: List<ReserveEntity>,
    onCompleteExpense: (Long) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    onUpdateReserve: (ReserveEntity) -> Unit,
    onDeleteReserve: (Long) -> Unit,
    onOpenPriceCalculator: () -> Unit,
    onOpenMarginCalculator: () -> Unit,
) {
    val openExpenses = expenses.filter { !it.isCompleted }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "План",
                subtitle = "Резервы, будущие расходы и цена вашей работы",
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickAction(
                    icon = Icons.Rounded.Calculate,
                    title = "Цена проекта",
                    subtitle = "Минимум и запас",
                    onClick = onOpenPriceCalculator,
                    modifier = Modifier.weight(1f),
                )
                QuickAction(
                    icon = Icons.Rounded.Percent,
                    title = "Маржа",
                    subtitle = "Чистая прибыль",
                    onClick = onOpenMarginCalculator,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Spacer(Modifier.height(2.dp))
            SectionHeader("Финансовые резервы")
        }

        if (reserves.isEmpty()) {
            item {
                EmptyState(
                    title = "Резервов пока нет",
                    description = "Создайте налоговый резерв или финансовую подушку",
                )
            }
        } else {
            items(reserves, key = { "s${it.id}" }) { reserve ->
                GroupCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    reserve.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    if (reserve.isProtected) {
                                        "Защищённый резерв"
                                    } else {
                                        "Не вычитается из доступного"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "${formatMoney(reserve.currentMinor, reserve.currency)} / ${formatMoney(reserve.targetMinor, reserve.currency)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        LinearProgressIndicator(
                            progress = {
                                if (reserve.targetMinor <= 0) {
                                    0f
                                } else {
                                    (reserve.currentMinor.toFloat() / reserve.targetMinor).coerceIn(0f, 1f)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { onUpdateReserve(reserve) }) {
                                Text("Изменить")
                            }
                            TextButton(onClick = { onDeleteReserve(reserve.id) }) {
                                Text("Удалить", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(2.dp))
            SectionHeader("Будущие расходы")
        }

        if (openExpenses.isEmpty()) {
            item {
                EmptyState(
                    title = "Нет будущих расходов",
                    description = "Добавьте аренду, подписки и другие обязательства",
                )
            }
        } else {
            items(openExpenses, key = { "p${it.id}" }) { expense ->
                GroupCard(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
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
                                "${expense.category} · ${LocalDate.ofEpochDay(expense.dueAtEpochDay)}${if (expense.recurrence != "NONE") " · повторяется" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatMoney(expense.amountMinor, expense.currency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Row {
                                IconButton(
                                    onClick = { onCompleteExpense(expense.id) },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Done,
                                        contentDescription = "Оплачено",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteExpense(expense.id) },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Удалить",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoreScreen(
    settings: UserSettings,
    onUpdateSettings: () -> Unit,
    onSetTheme: (String) -> Unit,
    onPrivacyMode: (Boolean) -> Unit,
    onExportCsv: (Uri) -> Unit,
    onExportPdf: (Uri) -> Unit,
    onBackup: (Uri) -> Unit,
    onRestore: (Uri) -> Unit,
    onClearData: () -> Unit,
) {
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) {
        it?.let(onExportCsv)
    }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) {
        it?.let(onExportPdf)
    }
    val backup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        it?.let(onBackup)
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let(onRestore)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Ещё",
                subtitle = "Настройки, отчёты и ваши данные",
            )
        }

        item { SectionHeader("Финансы") }
        item {
            GroupCard(Modifier.fillMaxWidth()) {
                SettingsRow(
                    icon = Icons.Rounded.Tune,
                    title = "Финансовые настройки",
                    subtitle = "${settings.baseCurrency} · резерв ${settings.taxPercent}%",
                    onClick = onUpdateSettings,
                )
            }
        }

        item { SectionHeader("Внешний вид") }
        item {
            GroupCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Тема",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeChoice(
                            title = "Авто",
                            icon = Icons.Rounded.BrightnessAuto,
                            selected = settings.darkMode == "SYSTEM",
                            onClick = { onSetTheme("SYSTEM") },
                            modifier = Modifier.weight(1f),
                        )
                        ThemeChoice(
                            title = "Светлая",
                            icon = Icons.Rounded.LightMode,
                            selected = settings.darkMode == "LIGHT",
                            onClick = { onSetTheme("LIGHT") },
                            modifier = Modifier.weight(1f),
                        )
                        ThemeChoice(
                            title = "Тёмная",
                            icon = Icons.Rounded.DarkMode,
                            selected = settings.darkMode == "DARK",
                            onClick = { onSetTheme("DARK") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item { SectionHeader("Экспорт и данные") }
        item {
            GroupCard(Modifier.fillMaxWidth()) {
                Column {
                    SettingsRow(
                        icon = Icons.Rounded.TableView,
                        title = "Экспорт CSV",
                        subtitle = "Для Excel и Google Sheets",
                        showDivider = true,
                    ) {
                        csv.launch("norvexa-flow-${LocalDate.now()}.csv")
                    }
                    SettingsRow(
                        icon = Icons.Rounded.PictureAsPdf,
                        title = "Финансовый отчёт PDF",
                        subtitle = "Сводка, оплаты и операции",
                        showDivider = true,
                    ) {
                        pdf.launch("norvexa-flow-report-${LocalDate.now()}.pdf")
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Backup,
                        title = "Создать резервную копию",
                        subtitle = "Локальный файл .nvxflow",
                        showDivider = true,
                    ) {
                        backup.launch("norvexa-flow-${LocalDate.now()}.nvxflow")
                    }
                    SettingsRow(
                        icon = Icons.Rounded.Restore,
                        title = "Восстановить копию",
                        subtitle = "Текущие данные будут заменены",
                    ) {
                        restore.launch(arrayOf("application/octet-stream", "application/json", "text/plain"))
                    }
                }
            }
        }

        item { SectionHeader("Конфиденциальность") }
        item {
            GroupCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconBubble(Icons.Rounded.Security)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            "Защита экрана",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Запретить скриншоты и превью в списке приложений",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.privacyMode,
                        onCheckedChange = onPrivacyMode,
                    )
                }
            }
        }

        item { SectionHeader("О приложении") }
        item {
            GroupCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Norvexa Flow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "0.2.0 alpha01 · локальный финансовый помощник",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Прогнозы и резервы служат для личного планирования и не являются бухгалтерской, налоговой, банковской или инвестиционной консультацией.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = onClearData,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Удалить все финансовые данные")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconBubble(icon)
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    showDivider: Boolean = false,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBubble(icon)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 64.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun ThemeChoice(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(46.dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.size(5.dp))
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(46.dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.size(5.dp))
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}
