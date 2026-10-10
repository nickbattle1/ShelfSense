package com.example.shelfsense.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.shelfsense.ShelfSenseApplication
import com.example.shelfsense.data.local.PantryItem
import com.example.shelfsense.data.local.actionInfo
import com.example.shelfsense.data.model.Outcome
import com.example.shelfsense.data.repository.PantryRepository
import com.example.shelfsense.domain.ActionInfo
import com.example.shelfsense.navigation.Routes
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val item: PantryItem? = null,
    val info: ActionInfo? = null
)

class FoodDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val pantry = PantryRepository(application)
    private val appScope = (application as ShelfSenseApplication).applicationScope
    private val itemId: String = checkNotNull(savedStateHandle.get<String>(Routes.ARG_ITEM_ID))

    val uiState: StateFlow<DetailUiState> = pantry.observeItem(itemId)
        .map { item -> DetailUiState(loading = false, item = item, info = item?.actionInfo()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    // stops a quick double tap recording an outcome or a delete twice
    private var busy = false

    // the write finishes before onDone runs, so navigating back can't cut it short
    fun recordOutcome(outcome: Outcome, onDone: (PantryItem) -> Unit) {
        val item = uiState.value.item ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                pantry.recordOutcome(item, outcome)
                onDone(item)
            } finally {
                busy = false
            }
        }
    }

    fun markOpened(date: LocalDate, useWithinDays: Int?) {
        val item = uiState.value.item ?: return
        viewModelScope.launch { pantry.markOpened(item, date, useWithinDays) }
    }

    fun delete(onDone: (PantryItem) -> Unit) {
        val item = uiState.value.item ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                pantry.delete(item)
                onDone(item)
            } finally {
                busy = false
            }
        }
    }

    // runs on the application scope because this screen has closed by the time Undo is tapped
    fun undo(snapshot: PantryItem) {
        appScope.launch { pantry.restore(snapshot) }
    }
}
