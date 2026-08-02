package com.norvexa.flow.di

import android.content.Context
import androidx.room.Room
import com.norvexa.flow.data.local.NorvexaDatabase
import com.norvexa.flow.data.local.dao.FinanceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): NorvexaDatabase = Room.databaseBuilder(
        context,
        NorvexaDatabase::class.java,
        "norvexa_flow.db",
    ).build()

    @Provides
    fun provideFinanceDao(database: NorvexaDatabase): FinanceDao = database.financeDao()
}
