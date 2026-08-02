package com.norvexa.flow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reserves")
data class ReserveEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val currentMinor: Long,
    val targetMinor: Long? = null,
    val currencyCode: String,
    val isProtected: Boolean = true,
    val targetAtEpochDay: Long? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
