package com.norvexa.flow.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receivables",
    indices = [Index("clientId"), Index("expectedAtEpochDay"), Index("status")],
)
data class ReceivableEntity(
    @PrimaryKey val id: String,
    val clientId: String,
    val projectId: String? = null,
    val title: String,
    val amountMinor: Long,
    val receivedMinor: Long = 0L,
    val currencyCode: String,
    val expectedAtEpochDay: Long,
    val paidAtEpochDay: Long? = null,
    val probabilityPercent: Int = 100,
    val status: String,
    val note: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
