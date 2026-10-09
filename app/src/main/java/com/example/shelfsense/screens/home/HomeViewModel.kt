package com.example.shelfsense.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelfsense.data.model.StorageLocation
import com.example.shelfsense.data.repository.PantryRepository
import com.example.shelfsense.data.repository.SettingsRepository
import com.example.shelfsense.domain.Dates
import com.example.shelfsense.domain.OutcomeTotals
import com.example.shelfsense.domain.TrackedItem
import com.example.shelfsense.domain.toTotals
import com.example.shelfsense.domain.tracked
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val firstName: String? = null,
    val counts: Map<StorageLocation, Int> = emptyMap(),
    val priority: List<TrackedItem> = emptyList(),
    val totalActive: Int = 0,
    val soonCount: Int = 0,
    val month: OutcomeTotals = OutcomeTotals(),
    val remindersEnabled: Boolean = true,
    val notificationAsked: Boolean = false,
    val bannerDismissed: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val pantry = PantryRepository(application)
    private val settings = SettingsRepository(application)
    private val monthStart = Dates.startOfDayMillis(LocalDate.now().withDayOfMonth(1))

    val uiState: StateFlow<HomeUiState> = combine(
        pantry.activeItems,
        pantry.observeOutcomeCounts(monthStart, Long.MAX_VALUE),
        settings.settings
    ) { items, monthCounts, prefs ->
        // Room already sorts by action date, so the first few are the ones to use first
        val tracked = items.tracked()
        val name = prefs.displayName ?: FirebaseAuth.getInstance().currentUser?.displayName
        HomeUiState(
            loading = false,
            firstName = name?.trim()?.substringBefore(' ')?.ifBlank { null },
            counts = items.groupingBy { it.storage }.eachCount(),
            priority = tracked.take(PRIORITY_COUNT),
            totalActive = items.size,
            soonCount = tracked.count { it.info.daysLeft <= prefs.leadDays },
            month = monthCounts.toTotals(),
            remindersEnabled = prefs.remindersEnabled,
            notificationAsked = prefs.notificationAsked,
            bannerDismissed = prefs.bannerDismissed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun markNotificationAsked() {
        viewModelScope.launch { settings.markNotificationAsked() }
    }

    fun dismissBanner() {
        viewModelScope.launch { settings.dismissBanner() }
    }

    private companion object {
        const val PRIORITY_COUNT = 5
    }
}
