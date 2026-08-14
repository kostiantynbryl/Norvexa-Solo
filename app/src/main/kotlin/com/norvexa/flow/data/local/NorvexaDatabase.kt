package com.norvexa.flow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WalletEntity::class,
        TransactionEntity::class,
        ClientEntity::class,
        ReceivableEntity::class,
        PlannedExpenseEntity::class,
        ReserveEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class NorvexaDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceType TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceId INTEGER")
            }
        }
    }
}
