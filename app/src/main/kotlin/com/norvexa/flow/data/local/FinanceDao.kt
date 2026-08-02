package com.norvexa.flow.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM wallets ORDER BY isActive DESC, name") fun observeWallets(): Flow<List<WalletEntity>>
    @Query("SELECT * FROM transactions ORDER BY occurredAtEpochMillis DESC") fun observeTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM clients ORDER BY isActive DESC, name") fun observeClients(): Flow<List<ClientEntity>>
    @Query("SELECT * FROM receivables ORDER BY expectedAtEpochDay, id") fun observeReceivables(): Flow<List<ReceivableEntity>>
    @Query("SELECT * FROM planned_expenses ORDER BY isCompleted, dueAtEpochDay, id") fun observePlannedExpenses(): Flow<List<PlannedExpenseEntity>>
    @Query("SELECT * FROM reserves ORDER BY name") fun observeReserves(): Flow<List<ReserveEntity>>

    @Query("SELECT * FROM wallets ORDER BY name") suspend fun getWallets(): List<WalletEntity>
    @Query("SELECT * FROM transactions ORDER BY occurredAtEpochMillis DESC") suspend fun getTransactions(): List<TransactionEntity>
    @Query("SELECT * FROM clients ORDER BY name") suspend fun getClients(): List<ClientEntity>
    @Query("SELECT * FROM receivables ORDER BY expectedAtEpochDay") suspend fun getReceivables(): List<ReceivableEntity>
    @Query("SELECT * FROM planned_expenses ORDER BY dueAtEpochDay") suspend fun getPlannedExpenses(): List<PlannedExpenseEntity>
    @Query("SELECT * FROM reserves ORDER BY name") suspend fun getReserves(): List<ReserveEntity>
    @Query("SELECT * FROM wallets WHERE id = :id LIMIT 1") suspend fun getWallet(id: Long): WalletEntity?
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1") suspend fun getTransaction(id: Long): TransactionEntity?
    @Query("SELECT * FROM receivables WHERE id = :id LIMIT 1") suspend fun getReceivable(id: Long): ReceivableEntity?
    @Query("SELECT * FROM planned_expenses WHERE id = :id LIMIT 1") suspend fun getPlannedExpense(id: Long): PlannedExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWallet(value: WalletEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTransaction(value: TransactionEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertClient(value: ClientEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReceivable(value: ReceivableEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPlannedExpense(value: PlannedExpenseEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReserve(value: ReserveEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWallets(values: List<WalletEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTransactions(values: List<TransactionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertClients(values: List<ClientEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReceivables(values: List<ReceivableEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPlannedExpenses(values: List<PlannedExpenseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReserves(values: List<ReserveEntity>)

    @Update suspend fun updateWallet(value: WalletEntity)
    @Update suspend fun updateReceivable(value: ReceivableEntity)
    @Update suspend fun updatePlannedExpense(value: PlannedExpenseEntity)
    @Update suspend fun updateReserve(value: ReserveEntity)
    @Delete suspend fun deleteTransaction(value: TransactionEntity)
    @Delete suspend fun deleteReceivable(value: ReceivableEntity)
    @Delete suspend fun deletePlannedExpense(value: PlannedExpenseEntity)
    @Delete suspend fun deleteReserve(value: ReserveEntity)

    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM receivables") suspend fun clearReceivables()
    @Query("DELETE FROM planned_expenses") suspend fun clearPlannedExpenses()
    @Query("DELETE FROM reserves") suspend fun clearReserves()
    @Query("DELETE FROM clients") suspend fun clearClients()
    @Query("DELETE FROM wallets") suspend fun clearWallets()
}
