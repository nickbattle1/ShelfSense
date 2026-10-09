package com.example.shelfsense.screens.pantry

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.shelfsense.data.repository.PantryRepository
import com.example.shelfsense.data.repository.SettingsRepository
import com.example.shelfsense.domain.PantryFilter
import com.example.shelfsense.domain.TrackedItem
import com.example.shelfsense.domain.tracked
import com.example.shelfsense.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PantryUiState(
    val loading: Boolean = true,
    val query: String = "",
    val filter: PantryFilter = PantryFilter.ALL,
    val items: List<TrackedItem> = emptyList(),
    val totalActive: Int = 0,
    val leadDays: Int = 2
)

class PantryViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val pantry = PantryRepository(application)
    private val settings = SettingsRepository(application)

    // kept in the SavedStateHandle so the search and filter survive rotation and tab switches.
    // the filter starts from the nav argument, so Home's shortcuts and the notification can preset it
    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val filter = savedStateHandle.getStateFlow(Routes.ARG_FILTER, PantryFilter.ALL.name)

    val uiState: StateFlow<PantryUiState> = combine(
        pantry.activeItems,
        settings.settings,
        query,
        filter
    ) { items, prefs, text, filterName ->
        val selected = PantryFilter.from(filterName)
        val needle = text.trim()
        val visible = items.tracked().filter { tracked ->
            val item = tracked.item
            val matchesFilter = selected.matches(
                tracked.info?.daysLeft, item.openedDate != null, item.storage, prefs.leadDays
            )
            val matchesText = needle.isEmpty() ||
                item.name.contains(needle, ignoreCase = true) ||
                item.brand?.contains(needle, ignoreCase = true) == true ||
                item.category.label.contains(needle, ignoreCase = true)
            matchesFilter && matchesText
        }
        PantryUiState(
            loading = false,
            query = text,
            filter = selected,
            items = visible,
            totalActive = items.size,
            leadDays = prefs.leadDays
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PantryUiState(filter = PantryFilter.from(savedStateHandle.get<String>(Routes.ARG_FILTER)))
    )

    fun onQueryChange(value: String) {
        savedStateHandle[KEY_QUERY] = value.take(60)
    }

    fun onFilterChange(value: PantryFilter) {
        savedStateHandle[Routes.ARG_FILTER] = value.name
    }

    fun clearSearch() {
        savedStateHandle[KEY_QUERY] = ""
        savedStateHandle[Routes.ARG_FILTER] = PantryFilter.ALL.name
    }

    private companion object {
        const val KEY_QUERY = "query"
    }
}
