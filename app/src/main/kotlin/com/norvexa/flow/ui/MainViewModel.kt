package com.norvexa.flow.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.norvexa.flow.data.export.ExportManager
import com.norvexa.flow.data.local.PlannedExpenseEntity
import com.norvexa.flow.data.local.ReceivableEntity
import com.norvexa.flow.data.local.ReserveEntity
import com.norvexa.flow.data.repository.FinanceRepository
import com.norvexa.flow.data.settings.SettingsStore
import com.norvexa.flow.data.settings.UserSettings
import com.norvexa.flow.domain.DashboardSummary
import com.norvexa.flow.domain.FinanceData
import com.norvexa.flow.domain.FinancialCalculator
import com.norvexa.flow.domain.MarginInput
import com.norvexa.flow.domain.MarginResult
import com.norvexa.flow.domain.PriceInput
import com.norvexa.flow.domain.PriceResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: FinanceRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    val financeData = repository.financeData.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FinanceData(),
    )
    val settings = settingsStore.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSettings(),
    )
    val dashboard = combine(financeData, settings) { data, userSettings ->
        FinancialCalculator.dashboard(
            wallets = data.wallets,
            transactions = data.transactions,
            receivables = data.receivables,
            expenses = data.plannedExpenses,
            reserves = data.reserves,
            taxPercent = userSettings.taxPercent,
            safeBalanceMinor = userSettings.safeBalanceMinor,
            baseCurrency = userSettings.baseCurrency,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardSummary(),
    )

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    fun completeOnboarding(
        currency: String,
        tax: Int,
        safe: Long,
        wallet: String,
        balance: Long,
    ) = launchAction("Профиль создан") {
        repository.addWallet(wallet, currency, balance, 1_000_000L)
        settingsStore.completeOnboarding(currency, tax, safe)
    }

    fun addWallet(name: String, currency: String, balance: Long, rate: Long) =
        launchAction("Кошелёк добавлен") {
            repository.addWallet(name, currency, balance, rate)
        }

    fun addTransaction(
        walletId: Long,
        type: String,
        amount: Long,
        category: String,
        note: String,
        clientId: Long?,
    ) = launchAction("Операция добавлена") {
        repository.addTransaction(walletId, type, amount, category, note, clientId)
    }

    fun deleteTransaction(id: Long) = launchAction("Операция удалена") {
        repository.deleteTransaction(id)
    }

    fun addClient(name: String, email: String, currency: String, note: String) =
        launchAction("Клиент добавлен") {
            repository.addClient(name, email, currency, note)
        }

    fun addReceivable(value: ReceivableEntity) = launchAction("Ожидаемая оплата добавлена") {
        repository.addReceivable(value)
    }

    fun settleReceivable(id: Long, walletId: Long) = launchAction("Оплата зачислена") {
        repository.settleReceivable(
            id = id,
            walletId = walletId,
            baseCurrency = settings.value.baseCurrency,
        )
    }

    fun deleteReceivable(id: Long) = launchAction("Ожидаемая оплата удалена") {
        repository.deleteReceivable(id)
    }

    fun addPlannedExpense(value: PlannedExpenseEntity) = launchAction("Расход запланирован") {
        repository.addPlannedExpense(value)
    }

    fun settlePlannedExpense(id: Long, walletId: Long) = launchAction("Расход оплачен") {
        repository.settlePlannedExpense(
            id = id,
            walletId = walletId,
            baseCurrency = settings.value.baseCurrency,
        )
    }

    fun deletePlannedExpense(id: Long) = launchAction("Расход удалён") {
        repository.deletePlannedExpense(id)
    }

    fun addReserve(value: ReserveEntity) = launchAction("Резерв создан") {
        repository.addReserve(value)
    }

    fun updateReserve(id: Long, amount: Long) = launchAction("Резерв обновлён") {
        repository.updateReserveAmount(id, amount)
    }

    fun deleteReserve(id: Long) = launchAction("Резерв удалён") {
        repository.deleteReserve(id)
    }

    fun updateSettings(tax: Int, safe: Long) = launchAction("Настройки сохранены") {
        settingsStore.updateFinancialSettings(tax, safe)
    }

    fun setTheme(value: String) = launchAction(null) { settingsStore.setTheme(value) }

    fun setPrivacyMode(value: Boolean) = launchAction(null) {
        settingsStore.setPrivacyMode(value)
    }

    fun calculatePrice(input: PriceInput): PriceResult = FinancialCalculator.calculatePrice(input)

    fun calculateMargin(input: MarginInput): MarginResult = FinancialCalculator.calculateMargin(input)

    fun exportCsv(context: Context, uri: Uri) = launchAction("CSV экспортирован") {
        ExportManager.writeCsv(context, uri, repository.snapshot())
    }

    fun exportPdf(context: Context, uri: Uri) = launchAction("PDF экспортирован") {
        ExportManager.writePdf(context, uri, repository.snapshot(), settings.value)
    }

    fun createBackup(context: Context, uri: Uri) = launchAction("Резервная копия создана") {
        ExportManager.writeBackup(context, uri, repository.snapshot(), settings.value)
    }

    fun restoreBackup(context: Context, uri: Uri) = launchAction("Резервная копия восстановлена") {
        val payload = ExportManager.readBackup(context, uri)
        if (payload.settings == null) {
            validateLegacyBackupBaseCurrency(payload.data, settings.value.baseCurrency)
        }
        repository.replaceAll(payload.data)
        payload.settings?.let {
            settingsStore.restoreFinancialSettings(
                baseCurrency = it.baseCurrency,
                taxPercent = it.taxPercent,
                safeBalanceMinor = it.safeBalanceMinor,
            )
        }
    }

    fun clearAll() = launchAction("Все финансовые данные удалены") {
        repository.clearAll()
    }

    private fun validateLegacyBackupBaseCurrency(data: FinanceData, currentBaseCurrency: String) {
        val currenciesWithBaseRate = buildList {
            addAll(data.wallets.filter { it.rateToBaseMicros == 1_000_000L }.map { it.currency })
            addAll(data.transactions.filter { it.rateToBaseMicros == 1_000_000L }.map { it.currency })
            addAll(data.receivables.filter { it.rateToBaseMicros == 1_000_000L }.map { it.currency })
            addAll(data.plannedExpenses.filter { it.rateToBaseMicros == 1_000_000L }.map { it.currency })
            addAll(data.reserves.filter { it.rateToBaseMicros == 1_000_000L }.map { it.currency })
        }.map { it.uppercase() }.toSet()

        require(currenciesWithBaseRate.isEmpty() || currenciesWithBaseRate == setOf(currentBaseCurrency.uppercase())) {
            "Старая копия v1 использует другую базовую валюту. Восстановите её в профиле с исходной валютой."
        }
    }

    private fun launchAction(success: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { if (success != null) _messages.emit(success) }
                .onFailure { _messages.emit(it.message ?: "Не удалось выполнить действие") }
        }
    }

    class Factory(
        private val repository: FinanceRepository,
        private val settingsStore: SettingsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository, settingsStore) as T
    }
}
