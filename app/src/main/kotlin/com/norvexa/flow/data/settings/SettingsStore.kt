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

data class UserSettings(val onboardingCompleted: Boolean = false, val baseCurrency: String = "USD", val taxPercent: Int = 10, val safeBalanceMinor: Long = 0, val darkMode: String = "SYSTEM", val privacyMode: Boolean = false)

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
        UserSettings(prefs[Keys.onboarding] ?: false, prefs[Keys.baseCurrency] ?: "USD", prefs[Keys.taxPercent] ?: 10, prefs[Keys.safeBalance] ?: 0, prefs[Keys.darkMode] ?: "SYSTEM", prefs[Keys.privacyMode] ?: false)
    }
    suspend fun completeOnboarding(baseCurrency: String, taxPercent: Int, safeBalanceMinor: Long) { context.settingsDataStore.edit { it[Keys.onboarding] = true; it[Keys.baseCurrency] = baseCurrency.uppercase(); it[Keys.taxPercent] = taxPercent.coerceIn(0,95); it[Keys.safeBalance] = safeBalanceMinor.coerceAtLeast(0) } }
    suspend fun updateFinancialSettings(baseCurrency: String, taxPercent: Int, safeBalanceMinor: Long) { context.settingsDataStore.edit { it[Keys.baseCurrency] = baseCurrency.uppercase(); it[Keys.taxPercent] = taxPercent.coerceIn(0,95); it[Keys.safeBalance] = safeBalanceMinor.coerceAtLeast(0) } }
    suspend fun setTheme(value: String) { context.settingsDataStore.edit { it[Keys.darkMode] = value } }
    suspend fun setPrivacyMode(value: Boolean) { context.settingsDataStore.edit { it[Keys.privacyMode] = value } }
}
