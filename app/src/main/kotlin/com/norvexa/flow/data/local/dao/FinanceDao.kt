package com.norvexa.flow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.norvexa.flow.data.local.entity.ClientEntity
import com.norvexa.flow.data.local.entity.PlannedExpenseEntity
import com.norvexa.flow.data.local.entity.ReceivableEntity
import com.norvexa.flow.data.local.entity.ReserveEntity
import com.norvexa.flow.data.local.entity.TransactionEntity
import com.norvexa.flow.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM wallets WHERE isActive = 1 ORDER BY name")
    fun observeWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM transactions ORDER BY occurredAtEpochMillis DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM clients WHERE isActive = 1 ORDER BY name")
    fun observeClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM receivables WHERE status NOT IN ('PAID', 'CANCELLED') ORDER BY expectedAtEpochDay")
    fun observeOpenReceivables(): Flow<List<ReceivableEntity>>

    @Query("SELECT * FROM planned_expenses WHERE isCompleted = 0 ORDER BY dueAtEpochDay")
    fun observeUpcomingExpenses(): Flow<List<PlannedExpenseEntity>>

    @Query("SELECT * FROM reserves ORDER BY name")
    fun observeReserves(): Flow<List<ReserveEntity>>

    @Upsert suspend fun upsertWallet(value: WalletEntity)
    @Upsert suspend fun upsertTransaction(value: TransactionEntity)
    @Upsert suspend fun upsertClient(value: ClientEntity)
    @Upsert suspend fun upsertReceivable(value: ReceivableEntity)
    @Upsert suspend fun upsertPlannedExpense(value: PlannedExpenseEntity)
    @Upsert suspend fun upsertReserve(value: ReserveEntity)

    @Delete suspend fun deleteTransaction(value: TransactionEntity)
    @Delete suspend fun deleteReceivable(value: ReceivableEntity)
    @Delete suspend fun deletePlannedExpense(value: PlannedExpenseEntity)
}
