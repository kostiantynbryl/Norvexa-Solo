package com.norvexa.flow.data.repository

import androidx.room.withTransaction
import com.norvexa.flow.data.local.*
import com.norvexa.flow.domain.FinanceData
import com.norvexa.flow.domain.ReceivableStatus
import com.norvexa.flow.domain.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FinanceRepository(private val database: NorvexaDatabase, private val dao: FinanceDao) {
    private data class PrimaryData(val wallets: List<WalletEntity>, val transactions: List<TransactionEntity>, val clients: List<ClientEntity>)
    private data class PlanningData(val receivables: List<ReceivableEntity>, val expenses: List<PlannedExpenseEntity>, val reserves: List<ReserveEntity>)
    val financeData: Flow<FinanceData> = combine(
        combine(dao.observeWallets(), dao.observeTransactions(), dao.observeClients()) { w, t, c -> PrimaryData(w,t,c) },
        combine(dao.observeReceivables(), dao.observePlannedExpenses(), dao.observeReserves()) { r, e, s -> PlanningData(r,e,s) },
    ) { p, q -> FinanceData(p.wallets, p.transactions, p.clients, q.receivables, q.expenses, q.reserves) }

    suspend fun addWallet(name: String, currency: String, balanceMinor: Long, rateToBaseMicros: Long): Long = dao.insertWallet(WalletEntity(name = name.trim(), currency = currency.uppercase(), balanceMinor = balanceMinor, rateToBaseMicros = rateToBaseMicros))
    suspend fun addTransaction(walletId: Long, type: String, amountMinor: Long, category: String, note: String, clientId: Long? = null, occurredAtEpochMillis: Long = System.currentTimeMillis()) {
        require(type == TransactionType.INCOME || type == TransactionType.EXPENSE); require(amountMinor > 0)
        database.withTransaction {
            val wallet = dao.getWallet(walletId) ?: error("Wallet not found")
            val signed = if (type == TransactionType.INCOME) amountMinor else -amountMinor
            dao.updateWallet(wallet.copy(balanceMinor = wallet.balanceMinor + signed))
            dao.insertTransaction(TransactionEntity(walletId = walletId, clientId = clientId, type = type, amountMinor = amountMinor, currency = wallet.currency, rateToBaseMicros = wallet.rateToBaseMicros, category = category.trim().ifEmpty { "Другое" }, note = note.trim(), occurredAtEpochMillis = occurredAtEpochMillis))
        }
    }
    suspend fun deleteTransaction(id: Long) { database.withTransaction { val v = dao.getTransaction(id) ?: return@withTransaction; dao.getWallet(v.walletId)?.let { w -> dao.updateWallet(w.copy(balanceMinor = w.balanceMinor + if (v.type == TransactionType.INCOME) -v.amountMinor else v.amountMinor)) }; dao.deleteTransaction(v) } }
    suspend fun addClient(name: String, email: String, currency: String, note: String): Long = dao.insertClient(ClientEntity(name = name.trim(), email = email.trim(), defaultCurrency = currency.uppercase(), note = note.trim()))
    suspend fun addReceivable(value: ReceivableEntity): Long = dao.insertReceivable(value)
    suspend fun markReceivablePaid(id: Long) { dao.getReceivable(id)?.let { dao.updateReceivable(it.copy(receivedMinor = it.amountMinor, status = ReceivableStatus.PAID)) } }
    suspend fun addPartialPayment(id: Long, amountMinor: Long) { dao.getReceivable(id)?.let { v -> val received = (v.receivedMinor + amountMinor).coerceAtMost(v.amountMinor); dao.updateReceivable(v.copy(receivedMinor = received, status = if (received >= v.amountMinor) ReceivableStatus.PAID else ReceivableStatus.PARTIAL)) } }
    suspend fun deleteReceivable(id: Long) { dao.getReceivable(id)?.let { dao.deleteReceivable(it) } }
    suspend fun addPlannedExpense(value: PlannedExpenseEntity): Long = dao.insertPlannedExpense(value)
    suspend fun markExpenseCompleted(id: Long) { dao.getPlannedExpense(id)?.let { dao.updatePlannedExpense(it.copy(isCompleted = true)) } }
    suspend fun deletePlannedExpense(id: Long) { dao.getPlannedExpense(id)?.let { dao.deletePlannedExpense(it) } }
    suspend fun addReserve(value: ReserveEntity): Long = dao.insertReserve(value)
    suspend fun updateReserveAmount(id: Long, newAmountMinor: Long) { dao.getReserves().firstOrNull { it.id == id }?.let { dao.updateReserve(it.copy(currentMinor = newAmountMinor.coerceAtLeast(0))) } }
    suspend fun deleteReserve(id: Long) { dao.getReserves().firstOrNull { it.id == id }?.let { dao.deleteReserve(it) } }
    suspend fun snapshot(): FinanceData = FinanceData(dao.getWallets(), dao.getTransactions(), dao.getClients(), dao.getReceivables(), dao.getPlannedExpenses(), dao.getReserves())
    suspend fun replaceAll(data: FinanceData) { database.withTransaction { dao.clearTransactions(); dao.clearReceivables(); dao.clearPlannedExpenses(); dao.clearReserves(); dao.clearClients(); dao.clearWallets(); dao.insertWallets(data.wallets); dao.insertClients(data.clients); dao.insertTransactions(data.transactions); dao.insertReceivables(data.receivables); dao.insertPlannedExpenses(data.plannedExpenses); dao.insertReserves(data.reserves) } }
    suspend fun clearAll() = replaceAll(FinanceData())
}
