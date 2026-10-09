package com.example.shelfsense.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.shelfsense.data.model.ThemeMode
import com.example.shelfsense.data.repository.AuthRepository
import com.example.shelfsense.data.repository.PantryRepository
import com.example.shelfsense.data.repository.SettingsRepository
import com.example.shelfsense.data.sample.SampleData
import com.example.shelfsense.worker.WorkScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val emailVerified: Boolean = true,
    val household: String? = null,
    val remindersEnabled: Boolean = true,
    val leadDays: Int = 2,
    val lastCheck: Long? = null,
    val lastCheckCount: Int = 0,
    val lastSync: Long? = null,
    val pendingCount: Int = 0,
    val syncing: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationAsked: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val auth = AuthRepository(application)
    private val pantry = PantryRepository(application)
    private val settings = SettingsRepository(application)

    // Firebase only refreshes this flag on request, so the screen asks again whenever it resumes
    private val verified = MutableStateFlow(auth.isEmailVerified)

    // sync status comes straight from WorkManager, so the row updates while the worker runs
    val uiState: StateFlow<ProfileUiState> = combine(
        settings.settings,
        pantry.observePendingCount(),
        WorkScheduler.observeSync(application),
        verified
    ) { prefs, pending, work, isVerified ->
        val user = auth.currentUser
        ProfileUiState(
            name = prefs.displayName ?: user?.displayName ?: "ShelfSense user",
            email = user?.email.orEmpty(),
            emailVerified = isVerified,
            household = prefs.householdSize,
            remindersEnabled = prefs.remindersEnabled,
            leadDays = prefs.leadDays,
            lastCheck = prefs.lastReminderCheck,
            lastCheckCount = prefs.lastReminderCount,
            lastSync = prefs.lastSync,
            pendingCount = pending,
            syncing = work.any { it.state == WorkInfo.State.RUNNING },
            themeMode = prefs.themeMode,
            notificationAsked = prefs.notificationAsked
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun refreshVerification() {
        viewModelScope.launch { verified.value = auth.refreshVerified() }
    }

    fun resendVerification(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val message = try {
                auth.sendVerification()
                "Verification email sent. Check your inbox."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AuthRepository.messageFor(e)
            }
            onResult(message)
        }
    }

    fun setReminders(enabled: Boolean) {
        viewModelScope.launch {
            settings.setRemindersEnabled(enabled)
            if (enabled) WorkScheduler.scheduleReminders(app) else WorkScheduler.cancelReminders(app)
        }
    }

    fun setLeadDays(days: Int) {
        viewModelScope.launch { settings.setLeadDays(days) }
    }

    fun checkNow() {
        WorkScheduler.checkRemindersNow(app)
    }

    fun syncNow() {
        WorkScheduler.requestSync(app, pull = true)
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun markNotificationAsked() {
        viewModelScope.launch { settings.markNotificationAsked() }
    }

    fun updateProfile(name: String, household: String) {
        viewModelScope.launch { auth.updateProfile(name.trim(), household) }
    }

    fun loadSampleData(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val items = SampleData.build()
            pantry.saveAll(items)
            onDone(items.size)
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            auth.signOut()
            onDone()
        }
    }
}
