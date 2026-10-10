package com.example.shelfsense.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelfsense.data.model.ThemeMode
import com.example.shelfsense.data.repository.AuthRepository
import com.example.shelfsense.data.repository.PantryRepository
import com.example.shelfsense.data.repository.SettingsRepository
import com.example.shelfsense.data.sample.SampleData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
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

// progress and error for the change password and delete account sheets
data class SheetStatus(val busy: Boolean = false, val error: String? = null)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = AuthRepository(application)
    private val pantry = PantryRepository(application)
    private val settings = SettingsRepository(application)

    // sync status follows the worker's state, so the row updates while it runs
    val uiState: StateFlow<ProfileUiState> = combine(
        settings.settings,
        pantry.observePendingCount(),
        pantry.observeSyncing()
    ) { prefs, pending, syncing ->
        val user = auth.currentUser
        ProfileUiState(
            name = prefs.displayName ?: user?.displayName ?: "ShelfSense user",
            email = user?.email.orEmpty(),
            household = prefs.householdSize,
            remindersEnabled = prefs.remindersEnabled,
            leadDays = prefs.leadDays,
            lastCheck = prefs.lastReminderCheck,
            lastCheckCount = prefs.lastReminderCount,
            lastSync = prefs.lastSync,
            pendingCount = pending,
            syncing = syncing,
            themeMode = prefs.themeMode,
            notificationAsked = prefs.notificationAsked
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    private val _passwordStatus = MutableStateFlow(SheetStatus())
    val passwordStatus: StateFlow<SheetStatus> = _passwordStatus.asStateFlow()

    private val _deleteStatus = MutableStateFlow(SheetStatus())
    val deleteStatus: StateFlow<SheetStatus> = _deleteStatus.asStateFlow()

    fun changePassword(current: String, newPassword: String, onDone: () -> Unit) {
        if (_passwordStatus.value.busy) return
        _passwordStatus.value = SheetStatus(busy = true)
        viewModelScope.launch {
            try {
                auth.changePassword(current, newPassword)
                _passwordStatus.value = SheetStatus()
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _passwordStatus.value = SheetStatus(error = AuthRepository.reauthMessageFor(e))
            }
        }
    }

    fun deleteAccount(password: String, onDone: () -> Unit) {
        if (_deleteStatus.value.busy) return
        _deleteStatus.value = SheetStatus(busy = true)
        viewModelScope.launch {
            try {
                auth.deleteAccount(password)
                _deleteStatus.value = SheetStatus()
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _deleteStatus.value = SheetStatus(error = AuthRepository.reauthMessageFor(e))
            }
        }
    }

    // a sheet closed part way through shouldn't reopen with an old error
    fun clearSheetStatus() {
        _passwordStatus.value = SheetStatus()
        _deleteStatus.value = SheetStatus()
    }

    fun setReminders(enabled: Boolean) {
        viewModelScope.launch { settings.setRemindersEnabled(enabled) }
    }

    fun setLeadDays(days: Int) {
        viewModelScope.launch { settings.setLeadDays(days) }
    }

    fun checkNow() {
        settings.checkRemindersNow()
    }

    fun syncNow() {
        pantry.requestSync(pull = true)
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
