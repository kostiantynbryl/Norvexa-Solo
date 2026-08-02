package com.norvexa.flow.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "planned_expenses",
    indices = [Index("dueAtEpochDay"), Index("walletId")],
)
data class PlannedExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amountMinor: Long,
    val currencyCode: String,
    val baseAmountMinor: Long,
    val baseCurrencyCode: String,
    val exchangeRate: String,
    val dueAtEpochDay: Long,
    val category: String,
    val walletId: String? = null,
    val isMandatory: Boolean = true,
    val recurrenceRule: String? = null,
    val isCompleted: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
