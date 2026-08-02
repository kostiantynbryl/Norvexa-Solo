package com.norvexa.flow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WalletEntity::class, TransactionEntity::class, ClientEntity::class, ReceivableEntity::class, PlannedExpenseEntity::class, ReserveEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class NorvexaDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
}
