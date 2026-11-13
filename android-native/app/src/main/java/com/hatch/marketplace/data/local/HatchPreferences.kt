package com.hatch.marketplace.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hatch.marketplace.data.model.AppLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.hatchDataStore by preferencesDataStore(name = "hatch_preferences")

@Singleton
class HatchPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val SELECTED_CITY = stringPreferencesKey("selected_city")
        val SEARCH_HISTORY = stringPreferencesKey("search_history_json")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val languageOverrideFlow: Flow<AppLanguage?> = context.hatchDataStore.data.map { prefs ->
        when (prefs[Keys.LANGUAGE]) {
            AppLanguage.EN.code -> AppLanguage.EN
            AppLanguage.AR.code -> AppLanguage.AR
            else -> null
        }
    }

    val languageFlow: Flow<AppLanguage> = languageOverrideFlow.map { manual ->
        manual ?: if (Locale.getDefault().language.equals("ar", ignoreCase = true)) {
            AppLanguage.AR
        } else {
            AppLanguage.EN
        }
    }

    val selectedCityFlow: Flow<String> = context.hatchDataStore.data.map { prefs ->
        prefs[Keys.SELECTED_CITY] ?: "all"
    }

    val searchHistoryFlow: Flow<List<String>> = context.hatchDataStore.data.map { prefs ->
        val raw = prefs[Keys.SEARCH_HISTORY].orEmpty()
        if (raw.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.hatchDataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.hatchDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setSelectedCity(city: String) {
        context.hatchDataStore.edit { it[Keys.SELECTED_CITY] = city }
    }

    suspend fun addSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        context.hatchDataStore.edit { prefs ->
            val current = prefs[Keys.SEARCH_HISTORY]?.let { raw ->
                runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
            }.orEmpty()
            val updated = (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) })
                .take(5)
            prefs[Keys.SEARCH_HISTORY] = json.encodeToString(updated)
        }
    }

    suspend fun removeSearchHistoryItem(query: String) {
        context.hatchDataStore.edit { prefs ->
            val current = prefs[Keys.SEARCH_HISTORY]?.let { raw ->
                runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
            }.orEmpty()
            val updated = current.filterNot { it == query }
            prefs[Keys.SEARCH_HISTORY] = json.encodeToString(updated)
        }
    }

    suspend fun clearSearchHistory() {
        context.hatchDataStore.edit { it.remove(Keys.SEARCH_HISTORY) }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.hatchDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
}
