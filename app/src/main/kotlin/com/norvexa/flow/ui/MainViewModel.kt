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
import com.norvexa.flow.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val repository:FinanceRepository,private val settingsStore:SettingsStore):ViewModel(){
    val financeData=repository.financeData.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),FinanceData())
    val settings=settingsStore.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),UserSettings())
    val dashboard=combine(financeData,settings){d,s->FinancialCalculator.dashboard(d.wallets,d.transactions,d.receivables,d.plannedExpenses,d.reserves,s.taxPercent,s.safeBalanceMinor)}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),DashboardSummary())
    private val _messages=MutableSharedFlow<String>(extraBufferCapacity=8);val messages=_messages.asSharedFlow()
    fun completeOnboarding(currency:String,tax:Int,safe:Long,wallet:String,balance:Long)=launchAction("Профиль создан"){repository.addWallet(wallet,currency,balance,1_000_000L);settingsStore.completeOnboarding(currency,tax,safe)}
    fun addWallet(n:String,c:String,b:Long,r:Long)=launchAction("Кошелёк добавлен"){repository.addWallet(n,c,b,r)}
    fun addTransaction(w:Long,t:String,a:Long,c:String,n:String,client:Long?)=launchAction("Операция добавлена"){repository.addTransaction(w,t,a,c,n,client)}
    fun deleteTransaction(id:Long)=launchAction("Операция удалена"){repository.deleteTransaction(id)}
    fun addClient(n:String,e:String,c:String,note:String)=launchAction("Клиент добавлен"){repository.addClient(n,e,c,note)}
    fun addReceivable(v:ReceivableEntity)=launchAction("Ожидаемая оплата добавлена"){repository.addReceivable(v)}
    fun markReceivablePaid(id:Long)=launchAction("Оплата отмечена полученной"){repository.markReceivablePaid(id)}
    fun addPartialPayment(id:Long,a:Long)=launchAction("Частичная оплата сохранена"){repository.addPartialPayment(id,a)}
    fun deleteReceivable(id:Long)=launchAction("Ожидаемая оплата удалена"){repository.deleteReceivable(id)}
    fun addPlannedExpense(v:PlannedExpenseEntity)=launchAction("Расход запланирован"){repository.addPlannedExpense(v)}
    fun markExpenseCompleted(id:Long)=launchAction("Расход отмечен выполненным"){repository.markExpenseCompleted(id)}
    fun deletePlannedExpense(id:Long)=launchAction("Расход удалён"){repository.deletePlannedExpense(id)}
    fun addReserve(v:ReserveEntity)=launchAction("Резерв создан"){repository.addReserve(v)}
    fun updateReserve(id:Long,a:Long)=launchAction("Резерв обновлён"){repository.updateReserveAmount(id,a)}
    fun deleteReserve(id:Long)=launchAction("Резерв удалён"){repository.deleteReserve(id)}
    fun updateSettings(c:String,t:Int,s:Long)=launchAction("Настройки сохранены"){settingsStore.updateFinancialSettings(c,t,s)}
    fun setTheme(v:String)=launchAction(null){settingsStore.setTheme(v)};fun setPrivacyMode(v:Boolean)=launchAction(null){settingsStore.setPrivacyMode(v)}
    fun calculatePrice(i:PriceInput)=FinancialCalculator.calculatePrice(i);fun calculateMargin(i:MarginInput)=FinancialCalculator.calculateMargin(i)
    fun exportCsv(c:Context,u:Uri)=launchAction("CSV экспортирован"){ExportManager.writeCsv(c,u,repository.snapshot())}
    fun exportPdf(c:Context,u:Uri)=launchAction("PDF экспортирован"){ExportManager.writePdf(c,u,repository.snapshot(),settings.value)}
    fun createBackup(c:Context,u:Uri)=launchAction("Резервная копия создана"){ExportManager.writeBackup(c,u,repository.snapshot())}
    fun restoreBackup(c:Context,u:Uri)=launchAction("Резервная копия восстановлена"){repository.replaceAll(ExportManager.readBackup(c,u))}
    fun clearAll()=launchAction("Все финансовые данные удалены"){repository.clearAll()}
    private fun launchAction(success:String?,block:suspend()->Unit){viewModelScope.launch{runCatching{block()}.onSuccess{if(success!=null)_messages.emit(success)}.onFailure{_messages.emit(it.message?:"Не удалось выполнить действие")}}}
    class Factory(private val repository:FinanceRepository,private val settingsStore:SettingsStore):ViewModelProvider.Factory{@Suppress("UNCHECKED_CAST")override fun<T:ViewModel>create(modelClass:Class<T>):T=MainViewModel(repository,settingsStore) as T}
}
