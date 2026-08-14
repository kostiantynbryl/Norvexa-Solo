package com.norvexa.flow.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "norvexa_flow_settings")

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val baseCurrency: String = "USD",
    val taxPercent: Int = 10,
    val safeBalanceMinor: Long = 0,
    val darkMode: String = "SYSTEM",
    val privacyMode: Boolean = false,
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_completed")
        val baseCurrency = stringPreferencesKey("base_currency")
        val taxPercent = intPreferencesKey("tax_percent")
        val safeBalance = longPreferencesKey("safe_balance_minor")
        val darkMode = stringPreferencesKey("dark_mode")
        val privacyMode = booleanPreferencesKey("privacy_mode")
    }

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            onboardingCompleted = prefs[Keys.onboarding] ?: false,
            baseCurrency = prefs[Keys.baseCurrency] ?: "USD",
            taxPercent = prefs[Keys.taxPercent] ?: 10,
            safeBalanceMinor = prefs[Keys.safeBalance] ?: 0,
            darkMode = prefs[Keys.darkMode] ?: "SYSTEM",
            privacyMode = prefs[Keys.privacyMode] ?: false,
        )
    }

    suspend fun completeOnboarding(
        baseCurrency: String,
        taxPercent: Int,
        safeBalanceMinor: Long,
    ) {
        context.settingsDataStore.edit {
            it[Keys.onboarding] = true
            it[Keys.baseCurrency] = baseCurrency.uppercase()
            it[Keys.taxPercent] = taxPercent.coerceIn(0, 95)
            it[Keys.safeBalance] = safeBalanceMinor.coerceAtLeast(0)
        }
    }

    /**
     * Base currency is intentionally immutable after onboarding. Changing it without
     * recalculating every stored historical exchange rate would corrupt aggregates.
     */
    suspend fun updateFinancialSettings(
        taxPercent: Int,
        safeBalanceMinor: Long,
    ) {
        context.settingsDataStore.edit {
            it[Keys.taxPercent] = taxPercent.coerceIn(0, 95)
            it[Keys.safeBalance] = safeBalanceMinor.coerceAtLeast(0)
        }
    }

    suspend fun restoreFinancialSettings(
        baseCurrency: String,
        taxPercent: Int,
        safeBalanceMinor: Long,
    ) {
        context.settingsDataStore.edit {
            it[Keys.onboarding] = true
            it[Keys.baseCurrency] = baseCurrency.uppercase()
            it[Keys.taxPercent] = taxPercent.coerceIn(0, 95)
            it[Keys.safeBalance] = safeBalanceMinor.coerceAtLeast(0)
        }
    }

    suspend fun setTheme(value: String) {
        require(value in setOf("SYSTEM", "LIGHT", "DARK"))
        context.settingsDataStore.edit { it[Keys.darkMode] = value }
    }

    suspend fun setPrivacyMode(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.privacyMode] = value }
    }
}
