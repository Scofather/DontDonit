package com.prismgrade.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Stores the user's Anthropic API key.
 *
 * The key lives in app-private storage and is excluded from backup (see
 * res/xml/backup_rules.xml), but it is still a credential on a device the user
 * controls. A shipped product should proxy requests through a server instead of
 * holding a key per install — see the security note in the README.
 */
class SettingsStore(private val context: Context) {

    val apiKey: Flow<String> = context.dataStore.data.map { it[API_KEY].orEmpty() }

    suspend fun setApiKey(value: String) {
        context.dataStore.edit { prefs -> prefs[API_KEY] = value.trim() }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { prefs -> prefs.remove(API_KEY) }
    }

    private companion object {
        val API_KEY = stringPreferencesKey("anthropic_api_key")
    }
}
