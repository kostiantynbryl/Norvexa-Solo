package com.norvexa.flow.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.norvexa.flow.data.local.ReserveEntity
import com.norvexa.flow.domain.TransactionType
import com.norvexa.flow.ui.components.AddClientDialog
import com.norvexa.flow.ui.components.AddExpenseDialog
import com.norvexa.flow.ui.components.AddReceivableDialog
import com.norvexa.flow.ui.components.AddReserveDialog
import com.norvexa.flow.ui.components.AddTransactionDialog
import com.norvexa.flow.ui.components.AddWalletDialog
import com.norvexa.flow.ui.components.ConfirmDialog
import com.norvexa.flow.ui.components.IconBubble
import com.norvexa.flow.ui.components.MarginCalculatorDialog
import com.norvexa.flow.ui.components.PriceCalculatorDialog
import com.norvexa.flow.ui.components.SettingsDialog
import com.norvexa.flow.ui.components.UpdateReserveDialog
import com.norvexa.flow.ui.screens.ActivityScreen
import com.norvexa.flow.ui.screens.ClientsScreen
import com.norvexa.flow.ui.screens.DashboardScreen
import com.norvexa.flow.ui.screens.MoreScreen
import com.norvexa.flow.ui.screens.OnboardingScreen
import com.norvexa.flow.ui.screens.PlanningScreen

private enum class MainSection(val title: String) {
    OVERVIEW("Обзор"),
    ACTIVITY("Операции"),
    CLIENTS("Клиенты"),
    PLANNING("План"),
    MORE("Ещё"),
}

private enum class DialogType {
    WALLET,
    INCOME,
    EXPENSE,
    CLIENT,
    RECEIVABLE,
    PLANNED_EXPENSE,
    RESERVE,
    SETTINGS,
    PRICE,
    MARGIN,
    CLEAR_DATA,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NorvexaFlowApp(viewModel: MainViewModel) {
    val data by viewModel.financeData.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    if (!settings.onboardingCompleted) {
        OnboardingScreen { currency, tax, safe, walletName, balance ->
            viewModel.completeOnboarding(currency, tax, safe, walletName, balance)
        }
        return
    }

    var sectionName by rememberSaveable { mutableStateOf(MainSection.OVERVIEW.name) }
    val section = MainSection.valueOf(sectionName)
    var showAddSheet by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<DialogType?>(null) }
    var reserveToUpdate by remember { mutableStateOf<ReserveEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(
                    modifier = Modifier.height(72.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    MainSection.entries.forEach { item ->
                        NavigationBarItem(
                            selected = section == item,
                            onClick = { sectionName = item.name },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            icon = {
                                Icon(
                                    imageVector = when (item) {
                                        MainSection.OVERVIEW -> Icons.Rounded.Home
                                        MainSection.ACTIVITY -> Icons.Rounded.SwapVert
                                        MainSection.CLIENTS -> Icons.Rounded.Groups
                                        MainSection.PLANNING -> Icons.Rounded.CalendarMonth
                                        MainSection.MORE -> Icons.Rounded.MoreHoriz
                                    },
                                    contentDescription = item.title,
                                    modifier = Modifier.size(22.dp),
                                )
                            },
                            label = {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (section != MainSection.MORE) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Добавить",
                        modifier = Modifier.size(25.dp),
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when (section) {
                MainSection.OVERVIEW -> DashboardScreen(
                    summary = dashboard,
                    currency = settings.baseCurrency,
                    receivables = data.receivables,
                    expenses = data.plannedExpenses,
                    clients = data.clients,
                    onPayReceivable = viewModel::markReceivablePaid,
                    onCompleteExpense = viewModel::markExpenseCompleted,
                )
                MainSection.ACTIVITY -> ActivityScreen(
                    transactions = data.transactions,
                    wallets = data.wallets,
                    clients = data.clients,
                    baseCurrency = settings.baseCurrency,
                    onDelete = viewModel::deleteTransaction,
                )
                MainSection.CLIENTS -> ClientsScreen(
                    clients = data.clients,
                    receivables = data.receivables,
                    onPaid = viewModel::markReceivablePaid,
                    onDelete = viewModel::deleteReceivable,
                )
                MainSection.PLANNING -> PlanningScreen(
                    expenses = data.plannedExpenses,
                    reserves = data.reserves,
                    onCompleteExpense = viewModel::markExpenseCompleted,
                    onDeleteExpense = viewModel::deletePlannedExpense,
                    onUpdateReserve = { reserveToUpdate = it },
                    onDeleteReserve = viewModel::deleteReserve,
                    onOpenPriceCalculator = { dialog = DialogType.PRICE },
                    onOpenMarginCalculator = { dialog = DialogType.MARGIN },
                )
                MainSection.MORE -> MoreScreen(
                    settings = settings,
                    onUpdateSettings = { dialog = DialogType.SETTINGS },
                    onSetTheme = viewModel::setTheme,
                    onPrivacyMode = viewModel::setPrivacyMode,
                    onExportCsv = { viewModel.exportCsv(context, it) },
                    onExportPdf = { viewModel.exportPdf(context, it) },
                    onBackup = { viewModel.createBackup(context, it) },
                    onRestore = { viewModel.restoreBackup(context, it) },
                    onClearData = { dialog = DialogType.CLEAR_DATA },
                )
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 28.dp),
            ) {
                Text(
                    "Добавить",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Выберите, что хотите записать",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                AddSheetItem(Icons.Rounded.Payments, "Доход", "Полученная оплата") {
                    dialog = DialogType.INCOME
                    showAddSheet = false
                }
                AddSheetItem(Icons.Rounded.ReceiptLong, "Расход", "Фактическая трата") {
                    dialog = DialogType.EXPENSE
                    showAddSheet = false
                }
                AddSheetItem(Icons.Rounded.AccountBalanceWallet, "Кошелёк", "Новый счёт или наличные") {
                    dialog = DialogType.WALLET
                    showAddSheet = false
                }
                AddSheetItem(Icons.Rounded.PersonAdd, "Клиент", "Контрагент или заказчик") {
                    dialog = DialogType.CLIENT
                    showAddSheet = false
                }
                AddSheetItem(Icons.Rounded.Payments, "Ожидаемая оплата", "Будущий доход") {
                    dialog = DialogType.RECEIVABLE
                    showAddSheet = false
                }
                AddSheetItem(Icons.Rounded.CalendarMonth, "Будущий расход", "Платёж или обязательство") {
                    dialog = DialogType.PLANNED_EXPENSE
                    showAddSheet = false
                }
                AddSheetItem(Icons.Rounded.Savings, "Финансовый резерв", "Подушка или отдельная цель") {
                    dialog = DialogType.RESERVE
                    showAddSheet = false
                }
            }
        }
    }

    when (dialog) {
        DialogType.WALLET -> AddWalletDialog(settings.baseCurrency, { dialog = null }, viewModel::addWallet)
        DialogType.INCOME -> AddTransactionDialog(
            data.wallets,
            data.clients,
            TransactionType.INCOME,
            { dialog = null },
            viewModel::addTransaction,
        )
        DialogType.EXPENSE -> AddTransactionDialog(
            data.wallets,
            data.clients,
            TransactionType.EXPENSE,
            { dialog = null },
            viewModel::addTransaction,
        )
        DialogType.CLIENT -> AddClientDialog(settings.baseCurrency, { dialog = null }, viewModel::addClient)
        DialogType.RECEIVABLE -> AddReceivableDialog(
            data.clients,
            settings.baseCurrency,
            { dialog = null },
            viewModel::addReceivable,
        )
        DialogType.PLANNED_EXPENSE -> AddExpenseDialog(
            settings.baseCurrency,
            { dialog = null },
            viewModel::addPlannedExpense,
        )
        DialogType.RESERVE -> AddReserveDialog(
            settings.baseCurrency,
            { dialog = null },
            viewModel::addReserve,
        )
        DialogType.SETTINGS -> SettingsDialog(settings, { dialog = null }, viewModel::updateSettings)
        DialogType.PRICE -> PriceCalculatorDialog(
            settings.baseCurrency,
            settings.taxPercent,
            viewModel::calculatePrice,
        ) { dialog = null }
        DialogType.MARGIN -> MarginCalculatorDialog(
            settings.baseCurrency,
            settings.taxPercent,
            viewModel::calculateMargin,
        ) { dialog = null }
        DialogType.CLEAR_DATA -> ConfirmDialog(
            title = "Удалить все данные?",
            text = "Кошельки, операции, клиенты, оплаты и резервы будут удалены. Перед этим рекомендуется создать резервную копию.",
            confirmLabel = "Удалить",
            onDismiss = { dialog = null },
        ) {
            viewModel.clearAll()
            dialog = null
        }
        null -> Unit
    }

    reserveToUpdate?.let { reserve ->
        UpdateReserveDialog(
            reserve = reserve,
            onDismiss = { reserveToUpdate = null },
        ) {
            viewModel.updateReserve(reserve.id, it)
            reserveToUpdate = null
        }
    }
}

@Composable
private fun AddSheetItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon = icon)
        Column(
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        ) {
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
