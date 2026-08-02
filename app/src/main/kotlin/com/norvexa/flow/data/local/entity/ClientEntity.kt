package com.norvexa.flow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val contactName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val messenger: String? = null,
    val currencyCode: String,
    val defaultPaymentTermDays: Int = 14,
    val note: String? = null,
    val isActive: Boolean = true,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
