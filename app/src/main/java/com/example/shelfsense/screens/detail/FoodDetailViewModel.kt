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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    // one-off messages for the snackbar when a write fails
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed()
            } finally {
                busy = false
            }
        }
    }

    fun markOpened(date: LocalDate, useWithinDays: Int?) {
        val item = uiState.value.item ?: return
        viewModelScope.launch {
            try {
                pantry.markOpened(item, date, useWithinDays)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed()
            }
        }
    }

    fun delete(onDone: (PantryItem) -> Unit) {
        val item = uiState.value.item ?: return
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                pantry.delete(item)
                onDone(item)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed()
            } finally {
                busy = false
            }
        }
    }

    // runs on the application scope because this screen has closed by the time Undo is tapped.
    // nothing is left on screen to report to, so a failed restore is dropped rather than crashing the app
    fun undo(snapshot: PantryItem) {
        appScope.launch { runCatching { pantry.restore(snapshot) } }
    }

    private fun failed() {
        _errors.tryEmit("That didn't save. Try again.")
    }
}
