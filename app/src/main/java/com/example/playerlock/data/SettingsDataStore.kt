package com.example.playerlock.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(val context: Context) {

    private companion object {
        val KEY_QUARTER_LENGTH = intPreferencesKey("quarter_length_minutes")
        val KEY_NORMALIZE_XP = booleanPreferencesKey("normalize_quarter_xp")
        val KEY_ACTIVE_PROFILE_ID = intPreferencesKey("active_profile_id")
        val KEY_SELECTED_THEME_ID = stringPreferencesKey("selected_theme_id")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_PURE_BLACK_AMOLED = booleanPreferencesKey("pure_black_amoled")
        val KEY_PERSONALIZE_PRIMARY_COLOR = longPreferencesKey("personalize_primary_color")
        val KEY_PERSONALIZE_SECONDARY_COLOR = longPreferencesKey("personalize_secondary_color")
        val KEY_AUTO_SAVE_ENABLED = booleanPreferencesKey("auto_save_enabled")
        val KEY_AUTO_SAVE_FOLDER_URI = stringPreferencesKey("auto_save_folder_uri")
        val KEY_LAST_AUTO_SAVE_TIMESTAMP = longPreferencesKey("last_auto_save_timestamp")
    }

    var quarterLengthMinutes: Int = 12
        private set
    var normalizeQuarterXp: Boolean = true
        private set

    /** Emits the persisted active-profile id, or null when none has been set yet. */
    val activeProfileId: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROFILE_ID]
    }

    /** Emits real-time app settings updates. */
    val appSettings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            quarterLengthMinutes = prefs[KEY_QUARTER_LENGTH] ?: 12,
            normalizeQuarterXp = prefs[KEY_NORMALIZE_XP] ?: true,
            selectedThemeId = prefs[KEY_SELECTED_THEME_ID] ?: "2k_gold",
            themeMode = prefs[KEY_THEME_MODE] ?: "DARK",
            pureBlackAmoled = prefs[KEY_PURE_BLACK_AMOLED] ?: false,
            personalizePrimaryColor = prefs[KEY_PERSONALIZE_PRIMARY_COLOR] ?: 0xFFFFB300L,
            personalizeSecondaryColor = prefs[KEY_PERSONALIZE_SECONDARY_COLOR] ?: 0xFF10B981L,
            autoSaveEnabled = prefs[KEY_AUTO_SAVE_ENABLED] ?: false,
            autoSaveFolderUri = prefs[KEY_AUTO_SAVE_FOLDER_URI],
            lastAutoSaveTimestamp = prefs[KEY_LAST_AUTO_SAVE_TIMESTAMP] ?: 0L
        )
    }

    suspend fun setActiveProfileId(id: Int?) {
        context.dataStore.edit { prefs ->
            if (id != null) {
                prefs[KEY_ACTIVE_PROFILE_ID] = id
            } else {
                prefs.remove(KEY_ACTIVE_PROFILE_ID)
            }
        }
    }

    suspend fun saveQuarterLengthMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(4, 12)
        quarterLengthMinutes = clamped
        context.dataStore.edit { it[KEY_QUARTER_LENGTH] = clamped }
    }

    suspend fun saveNormalizeQuarterXp(enabled: Boolean) {
        normalizeQuarterXp = enabled
        context.dataStore.edit { it[KEY_NORMALIZE_XP] = enabled }
    }

    suspend fun saveSelectedThemeId(id: String) {
        context.dataStore.edit { it[KEY_SELECTED_THEME_ID] = id }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun savePureBlackAmoled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PURE_BLACK_AMOLED] = enabled }
    }

    suspend fun savePersonalizeColors(primary: Long, secondary: Long) {
        context.dataStore.edit {
            it[KEY_PERSONALIZE_PRIMARY_COLOR] = primary
            it[KEY_PERSONALIZE_SECONDARY_COLOR] = secondary
        }
    }

    suspend fun saveAutoSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_SAVE_ENABLED] = enabled }
    }

    suspend fun saveAutoSaveFolderUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri != null) {
                prefs[KEY_AUTO_SAVE_FOLDER_URI] = uri
            } else {
                prefs.remove(KEY_AUTO_SAVE_FOLDER_URI)
            }
        }
    }

    suspend fun saveLastAutoSaveTimestamp(timestamp: Long) {
        context.dataStore.edit { it[KEY_LAST_AUTO_SAVE_TIMESTAMP] = timestamp }
    }

    suspend fun loadSettings(): AppSettings {
        val prefs = context.dataStore.data.first()
        quarterLengthMinutes = prefs[KEY_QUARTER_LENGTH] ?: 12
        normalizeQuarterXp = prefs[KEY_NORMALIZE_XP] ?: true
        return AppSettings(
            quarterLengthMinutes = quarterLengthMinutes,
            normalizeQuarterXp = normalizeQuarterXp,
            selectedThemeId = prefs[KEY_SELECTED_THEME_ID] ?: "2k_gold",
            themeMode = prefs[KEY_THEME_MODE] ?: "DARK",
            pureBlackAmoled = prefs[KEY_PURE_BLACK_AMOLED] ?: false,
            personalizePrimaryColor = prefs[KEY_PERSONALIZE_PRIMARY_COLOR] ?: 0xFFFFB300L,
            personalizeSecondaryColor = prefs[KEY_PERSONALIZE_SECONDARY_COLOR] ?: 0xFF10B981L,
            autoSaveEnabled = prefs[KEY_AUTO_SAVE_ENABLED] ?: false,
            autoSaveFolderUri = prefs[KEY_AUTO_SAVE_FOLDER_URI],
            lastAutoSaveTimestamp = prefs[KEY_LAST_AUTO_SAVE_TIMESTAMP] ?: 0L
        )
    }
}
