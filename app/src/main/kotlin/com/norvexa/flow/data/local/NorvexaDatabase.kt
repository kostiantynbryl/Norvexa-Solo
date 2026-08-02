package com.norvexa.flow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.norvexa.flow.data.local.dao.FinanceDao
import com.norvexa.flow.data.local.entity.ClientEntity
import com.norvexa.flow.data.local.entity.PlannedExpenseEntity
import com.norvexa.flow.data.local.entity.ReceivableEntity
import com.norvexa.flow.data.local.entity.ReserveEntity
import com.norvexa.flow.data.local.entity.TransactionEntity
import com.norvexa.flow.data.local.entity.WalletEntity

@Database(
    entities = [
        WalletEntity::class,
        TransactionEntity::class,
        ClientEntity::class,
        ReceivableEntity::class,
        PlannedExpenseEntity::class,
        ReserveEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NorvexaDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
}
