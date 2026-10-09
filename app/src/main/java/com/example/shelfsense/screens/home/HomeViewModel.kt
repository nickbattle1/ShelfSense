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
    // up to three items due within the next week, soonest first
    val dueSoon: List<TrackedItem> = emptyList(),
    // the next dated item after this week, only filled when nothing is due sooner
    val nextUp: TrackedItem? = null,
    val totalActive: Int = 0,
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
        // Room already sorts by action date, so these come out soonest first.
        // undated items have nothing to count down to, so they never appear here
        val dated = items.tracked().filter { it.info != null }
        val thisWeek = dated.filter { (it.info?.daysLeft ?: Long.MAX_VALUE) <= WINDOW_DAYS }
        val name = prefs.displayName ?: FirebaseAuth.getInstance().currentUser?.displayName
        HomeUiState(
            loading = false,
            firstName = name?.trim()?.substringBefore(' ')?.ifBlank { null },
            counts = items.groupingBy { it.storage }.eachCount(),
            dueSoon = thisWeek.take(MAX_SHOWN),
            nextUp = if (thisWeek.isEmpty()) dated.firstOrNull() else null,
            totalActive = items.size,
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
        // Home is a short summary of the week ahead, See all opens the full list in Pantry.
        // three cards keep the impact card on screen, as in the A1 layout
        const val WINDOW_DAYS = 7L
        const val MAX_SHOWN = 3
    }
}
