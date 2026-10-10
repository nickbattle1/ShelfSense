package com.example.shelfsense.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.shelfsense.data.model.ThemeMode
import com.example.shelfsense.worker.WorkScheduler
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "shelfsense_settings")

data class AppSettings(
    val remindersEnabled: Boolean = true,
    val leadDays: Int = 2,
    val lastReminderCheck: Long? = null,
    val lastReminderCount: Int = 0,
    val lastSync: Long? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val displayName: String? = null,
    val householdSize: String? = null,
    val profilePending: Boolean = false,
    val notificationAsked: Boolean = false,
    val bannerDismissed: Boolean = false
)

// reminder preferences, the theme choice and a small cache of the signed in profile, kept in Preferences DataStore
class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val store = appContext.settingsStore

    val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                remindersEnabled = prefs[REMINDERS_ENABLED] ?: true,
                leadDays = prefs[LEAD_DAYS] ?: 2,
                lastReminderCheck = prefs[LAST_REMINDER_CHECK],
                lastReminderCount = prefs[LAST_REMINDER_COUNT] ?: 0,
                lastSync = prefs[LAST_SYNC],
                themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[THEME_MODE] } ?: ThemeMode.SYSTEM,
                displayName = prefs[DISPLAY_NAME],
                householdSize = prefs[HOUSEHOLD_SIZE],
                profilePending = prefs[PROFILE_PENDING] ?: false,
                notificationAsked = prefs[NOTIFICATION_ASKED] ?: false,
                bannerDismissed = prefs[BANNER_DISMISSED] ?: false
            )
        }

    val themeMode: Flow<ThemeMode> = settings.map { it.themeMode }.distinctUntilChanged()

    // the daily check only exists while reminders are switched on
    suspend fun setRemindersEnabled(enabled: Boolean) {
        store.edit { it[REMINDERS_ENABLED] = enabled }
        if (enabled) WorkScheduler.scheduleReminders(appContext) else WorkScheduler.cancelReminders(appContext)
    }

    // Profile's "Check now", which runs the same reminder worker once, straight away
    fun checkRemindersNow() {
        WorkScheduler.checkRemindersNow(appContext)
    }

    // called at app start. the scheduler uses KEEP, so an existing schedule is left alone
    suspend fun restoreReminderSchedule() {
        if (settings.first().remindersEnabled) WorkScheduler.scheduleReminders(appContext)
    }

    suspend fun setLeadDays(days: Int) {
        store.edit { it[LEAD_DAYS] = days }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun markNotificationAsked() {
        store.edit { it[NOTIFICATION_ASKED] = true }
    }

    suspend fun dismissBanner() {
        store.edit { it[BANNER_DISMISSED] = true }
    }

    suspend fun recordReminderCheck(at: Long, dueCount: Int) {
        store.edit {
            it[LAST_REMINDER_CHECK] = at
            it[LAST_REMINDER_COUNT] = dueCount
        }
    }

    suspend fun recordSync(at: Long) {
        store.edit { it[LAST_SYNC] = at }
    }

    // pending = true means the Firestore copy still needs updating, the sync worker picks it up
    suspend fun saveProfile(name: String, household: String?, pending: Boolean) {
        store.edit {
            it[DISPLAY_NAME] = name
            if (household != null) it[HOUSEHOLD_SIZE] = household
            it[PROFILE_PENDING] = pending
        }
    }

    suspend fun markProfileSynced() {
        store.edit { it[PROFILE_PENDING] = false }
    }

    // reminder and theme choices stay, they belong to the device rather than the account
    suspend fun clearAccount() {
        store.edit {
            it.remove(DISPLAY_NAME)
            it.remove(HOUSEHOLD_SIZE)
            it.remove(PROFILE_PENDING)
            it.remove(LAST_SYNC)
            it.remove(LAST_REMINDER_COUNT)
            it.remove(BANNER_DISMISSED)
        }
    }

    private companion object {
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val LEAD_DAYS = intPreferencesKey("reminder_lead_days")
        val LAST_REMINDER_CHECK = longPreferencesKey("last_reminder_check")
        val LAST_REMINDER_COUNT = intPreferencesKey("last_reminder_count")
        val LAST_SYNC = longPreferencesKey("last_sync")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val HOUSEHOLD_SIZE = stringPreferencesKey("household_size")
        val PROFILE_PENDING = booleanPreferencesKey("profile_pending")
        val NOTIFICATION_ASKED = booleanPreferencesKey("notification_asked")
        val BANNER_DISMISSED = booleanPreferencesKey("permission_banner_dismissed")
    }
}
