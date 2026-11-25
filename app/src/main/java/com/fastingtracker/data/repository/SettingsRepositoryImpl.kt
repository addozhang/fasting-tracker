package com.fastingtracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fastingtracker.domain.model.AppLanguage
import com.fastingtracker.domain.model.AppTheme
import com.fastingtracker.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val DISCLAIMER_ACCEPTED_KEY = stringPreferencesKey("disclaimer_accepted")
        private val SELECTED_PLAN_ID_KEY = longPreferencesKey("selected_plan_id")
        private val SUPER_ISLAND_ENABLED_KEY = booleanPreferencesKey("super_island_enabled")
        private val LOCK_SCREEN_VISIBLE_KEY = booleanPreferencesKey("lock_screen_visible")
        private val MINIMAL_NOTIFICATION_KEY = booleanPreferencesKey("minimal_notification")
    }

    override fun observeTheme(): Flow<AppTheme> {
        return context.dataStore.data.map { preferences ->
            val themeString = preferences[THEME_KEY] ?: AppTheme.SYSTEM.name
            try {
                AppTheme.valueOf(themeString)
            } catch (_: IllegalArgumentException) {
                AppTheme.SYSTEM
            }
        }
    }

    override suspend fun getTheme(): AppTheme = observeTheme().first()

    override suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    override fun observeDynamicColor(): Flow<Boolean> {
        // Default OFF to preserve the Organic Minimal palette
        return context.dataStore.data.map { preferences ->
            preferences[DYNAMIC_COLOR_KEY] ?: false
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    override fun observeLanguage(): Flow<AppLanguage> {
        return context.dataStore.data.map { preferences ->
            val languageString = preferences[LANGUAGE_KEY] ?: AppLanguage.SYSTEM.name
            try {
                AppLanguage.valueOf(languageString)
            } catch (_: IllegalArgumentException) {
                AppLanguage.SYSTEM
            }
        }
    }

    override suspend fun getLanguage(): AppLanguage = observeLanguage().first()

    override suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.name
        }
    }

    override suspend fun hasAcceptedDisclaimer(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[DISCLAIMER_ACCEPTED_KEY] == "true"
    }

    override suspend fun setDisclaimerAccepted() {
        context.dataStore.edit { preferences ->
            preferences[DISCLAIMER_ACCEPTED_KEY] = "true"
        }
    }

    override fun observeSelectedPlanId(): Flow<Long?> {
        return context.dataStore.data.map { preferences ->
            preferences[SELECTED_PLAN_ID_KEY]
        }
    }

    override suspend fun getSelectedPlanId(): Long? = observeSelectedPlanId().first()

    override suspend fun setSelectedPlanId(planId: Long) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_PLAN_ID_KEY] = planId
        }
    }

    override fun observeSuperIslandEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[SUPER_ISLAND_ENABLED_KEY] ?: true
        }
    }

    override suspend fun isSuperIslandEnabled(): Boolean = observeSuperIslandEnabled().first()

    override suspend fun setSuperIslandEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SUPER_ISLAND_ENABLED_KEY] = enabled
        }
    }

    override fun observeLockScreenVisible(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[LOCK_SCREEN_VISIBLE_KEY] ?: true
        }
    }

    override suspend fun isLockScreenVisible(): Boolean = observeLockScreenVisible().first()

    override suspend fun setLockScreenVisible(visible: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LOCK_SCREEN_VISIBLE_KEY] = visible
        }
    }

    override fun observeMinimalNotification(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[MINIMAL_NOTIFICATION_KEY] ?: true
        }
    }

    override suspend fun isMinimalNotification(): Boolean = observeMinimalNotification().first()

    override suspend fun setMinimalNotification(minimal: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MINIMAL_NOTIFICATION_KEY] = minimal
        }
    }
}
