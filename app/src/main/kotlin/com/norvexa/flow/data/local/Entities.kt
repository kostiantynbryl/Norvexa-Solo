package com.norvexa.flow.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currency: String,
    val balanceMinor: Long,
    val rateToBaseMicros: Long = 1_000_000L,
    val isActive: Boolean = true,
)

@Entity(
    tableName = "transactions",
    indices = [Index("walletId"), Index("clientId")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val walletId: Long,
    val clientId: Long? = null,
    val type: String,
    val amountMinor: Long,
    val currency: String,
    val rateToBaseMicros: Long = 1_000_000L,
    val category: String,
    val note: String = "",
    val occurredAtEpochMillis: Long = System.currentTimeMillis(),
    val sourceType: String? = null,
    val sourceId: Long? = null,
)

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String = "",
    val defaultCurrency: String,
    val note: String = "",
    val isActive: Boolean = true,
)

@Entity(tableName = "receivables", indices = [Index("clientId")])
data class ReceivableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val title: String,
    val amountMinor: Long,
    val receivedMinor: Long = 0,
    val currency: String,
    val rateToBaseMicros: Long = 1_000_000L,
    val expectedAtEpochDay: Long,
    val probabilityPercent: Int = 100,
    val status: String = "EXPECTED",
    val note: String = "",
)

@Entity(tableName = "planned_expenses")
data class PlannedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountMinor: Long,
    val currency: String,
    val rateToBaseMicros: Long = 1_000_000L,
    val dueAtEpochDay: Long,
    val category: String,
    val isMandatory: Boolean = true,
    val isCompleted: Boolean = false,
    val recurrence: String = "NONE",
    val note: String = "",
)

@Entity(tableName = "reserves")
data class ReserveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetMinor: Long,
    val currentMinor: Long,
    val currency: String,
    val rateToBaseMicros: Long = 1_000_000L,
    val type: String = "CUSTOM",
    val isProtected: Boolean = true,
)
