package com.norvexa.flow.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("walletId"),
        Index("clientId"),
        Index("projectId"),
        Index("occurredAtEpochMillis"),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val walletId: String,
    val type: String,
    val amountMinor: Long,
    val currencyCode: String,
    val baseAmountMinor: Long,
    val baseCurrencyCode: String,
    val exchangeRate: String,
    val category: String,
    val clientId: String? = null,
    val projectId: String? = null,
    val note: String? = null,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
