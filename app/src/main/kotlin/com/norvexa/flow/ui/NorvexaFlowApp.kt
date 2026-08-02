package com.norvexa.flow.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.norvexa.flow.data.local.ReserveEntity
import com.norvexa.flow.domain.TransactionType
import com.norvexa.flow.ui.components.*
import com.norvexa.flow.ui.screens.*

private enum class MainSection(val title: String) { OVERVIEW("Обзор"), ACTIVITY("Операции"), CLIENTS("Клиенты"), PLANNING("План"), MORE("Ещё") }
private enum class DialogType { WALLET, INCOME, EXPENSE, CLIENT, RECEIVABLE, PLANNED_EXPENSE, RESERVE, SETTINGS, PRICE, MARGIN, CLEAR_DATA }

@Composable
fun NorvexaFlowApp(viewModel: MainViewModel) {
    val data by viewModel.financeData.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }
    if (!settings.onboardingCompleted) {
        OnboardingScreen { currency, tax, safe, walletName, balance -> viewModel.completeOnboarding(currency, tax, safe, walletName, balance) }
        return
    }
    var sectionName by rememberSaveable { mutableStateOf(MainSection.OVERVIEW.name) }
    val section = MainSection.valueOf(sectionName)
    var showAddSheet by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<DialogType?>(null) }
    var reserveToUpdate by remember { mutableStateOf<ReserveEntity?>(null) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { NavigationBar { MainSection.entries.forEach { item -> NavigationBarItem(selected = section == item, onClick = { sectionName = item.name }, icon = { Icon(when (item) { MainSection.OVERVIEW -> Icons.Rounded.Home; MainSection.ACTIVITY -> Icons.Rounded.SwapVert; MainSection.CLIENTS -> Icons.Rounded.Groups; MainSection.PLANNING -> Icons.Rounded.CalendarMonth; MainSection.MORE -> Icons.Rounded.MoreHoriz }, contentDescription = item.title) }, label = { Text(item.title) }) } } },
        floatingActionButton = { if (section != MainSection.MORE) FloatingActionButton(onClick = { showAddSheet = true }) { Icon(Icons.Rounded.Add, contentDescription = "Добавить") } },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (section) {
                MainSection.OVERVIEW -> DashboardScreen(dashboard, settings.baseCurrency, data.receivables, data.plannedExpenses, data.clients, viewModel::markReceivablePaid, viewModel::markExpenseCompleted)
                MainSection.ACTIVITY -> ActivityScreen(data.transactions, data.wallets, data.clients, settings.baseCurrency, viewModel::deleteTransaction)
                MainSection.CLIENTS -> ClientsScreen(data.clients, data.receivables, viewModel::markReceivablePaid, viewModel::deleteReceivable)
                MainSection.PLANNING -> PlanningScreen(data.plannedExpenses, data.reserves, viewModel::markExpenseCompleted, viewModel::deletePlannedExpense, { reserveToUpdate = it }, viewModel::deleteReserve, { dialog = DialogType.PRICE }, { dialog = DialogType.MARGIN })
                MainSection.MORE -> MoreScreen(settings, { dialog = DialogType.SETTINGS }, viewModel::setTheme, viewModel::setPrivacyMode, { viewModel.exportCsv(context, it) }, { viewModel.exportPdf(context, it) }, { viewModel.createBackup(context, it) }, { viewModel.restoreBackup(context, it) }, { dialog = DialogType.CLEAR_DATA })
            }
        }
    }
    if (showAddSheet) ModalBottomSheet(onDismissRequest = { showAddSheet = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text("Добавить", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge)
            AddSheetItem(Icons.Rounded.Payments, "Доход") { dialog = DialogType.INCOME; showAddSheet = false }
            AddSheetItem(Icons.Rounded.ReceiptLong, "Расход") { dialog = DialogType.EXPENSE; showAddSheet = false }
            AddSheetItem(Icons.Rounded.AccountBalanceWallet, "Кошелёк") { dialog = DialogType.WALLET; showAddSheet = false }
            AddSheetItem(Icons.Rounded.PersonAdd, "Клиент") { dialog = DialogType.CLIENT; showAddSheet = false }
            AddSheetItem(Icons.Rounded.Payments, "Ожидаемая оплата") { dialog = DialogType.RECEIVABLE; showAddSheet = false }
            AddSheetItem(Icons.Rounded.CalendarMonth, "Будущий расход") { dialog = DialogType.PLANNED_EXPENSE; showAddSheet = false }
            AddSheetItem(Icons.Rounded.Savings, "Финансовый резерв") { dialog = DialogType.RESERVE; showAddSheet = false }
        }
    }
    when (dialog) {
        DialogType.WALLET -> AddWalletDialog(settings.baseCurrency, { dialog = null }, viewModel::addWallet)
        DialogType.INCOME -> AddTransactionDialog(data.wallets, data.clients, TransactionType.INCOME, { dialog = null }, viewModel::addTransaction)
        DialogType.EXPENSE -> AddTransactionDialog(data.wallets, data.clients, TransactionType.EXPENSE, { dialog = null }, viewModel::addTransaction)
        DialogType.CLIENT -> AddClientDialog(settings.baseCurrency, { dialog = null }, viewModel::addClient)
        DialogType.RECEIVABLE -> AddReceivableDialog(data.clients, settings.baseCurrency, { dialog = null }, viewModel::addReceivable)
        DialogType.PLANNED_EXPENSE -> AddExpenseDialog(settings.baseCurrency, { dialog = null }, viewModel::addPlannedExpense)
        DialogType.RESERVE -> AddReserveDialog(settings.baseCurrency, { dialog = null }, viewModel::addReserve)
        DialogType.SETTINGS -> SettingsDialog(settings, { dialog = null }, viewModel::updateSettings)
        DialogType.PRICE -> PriceCalculatorDialog(settings.baseCurrency, settings.taxPercent, viewModel::calculatePrice) { dialog = null }
        DialogType.MARGIN -> MarginCalculatorDialog(settings.baseCurrency, settings.taxPercent, viewModel::calculateMargin) { dialog = null }
        DialogType.CLEAR_DATA -> ConfirmDialog("Удалить все данные?", "Кошельки, операции, клиенты, оплаты и резервы будут удалены. Перед этим рекомендуется создать резервную копию.", "Удалить", { dialog = null }) { viewModel.clearAll(); dialog = null }
        null -> Unit
    }
    reserveToUpdate?.let { reserve -> UpdateReserveDialog(reserve, { reserveToUpdate = null }) { viewModel.updateReserve(reserve.id, it); reserveToUpdate = null } }
}

@Composable
private fun AddSheetItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(title) }, leadingContent = { Icon(icon, contentDescription = null) }, modifier = Modifier.padding(horizontal = 8.dp).clickable(onClick = onClick))
    HorizontalDivider()
}
