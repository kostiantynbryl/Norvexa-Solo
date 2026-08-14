package com.norvexa.flow.data.repository

import android.content.Context
import androidx.room.Room
import com.norvexa.flow.data.local.NorvexaDatabase
import com.norvexa.flow.data.settings.SettingsStore

class AppContainer(context: Context) {
    val database: NorvexaDatabase = Room.databaseBuilder(
        context.applicationContext,
        NorvexaDatabase::class.java,
        "norvexa-flow.db",
    )
        .addMigrations(NorvexaDatabase.MIGRATION_1_2)
        .build()

    val settingsStore = SettingsStore(context.applicationContext)
    val repository = FinanceRepository(database, database.financeDao())
}
