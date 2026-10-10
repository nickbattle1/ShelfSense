package com.example.shelfsense.screens.addfood

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.shelfsense.data.local.PantryItem
import com.example.shelfsense.data.model.Choices
import com.example.shelfsense.data.model.DateType
import com.example.shelfsense.data.model.FoodCategory
import com.example.shelfsense.data.model.StorageLocation
import com.example.shelfsense.data.photos.PhotoStore
import com.example.shelfsense.data.repository.PantryRepository
import com.example.shelfsense.data.repository.ProductInfo
import com.example.shelfsense.data.repository.ProductLookup
import com.example.shelfsense.data.repository.ProductRepository
import com.example.shelfsense.domain.ActionDates
import com.example.shelfsense.domain.ActionInfo
import com.example.shelfsense.navigation.Routes
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FormMode { MANUAL, LOOKUP, EDIT }

// matches the field's limit, and stays well inside the 100 characters Firestore's rules allow
const val NAME_MAX = 60

sealed interface LookupState {
    data object None : LookupState
    data object Loading : LookupState
    data class Found(val product: ProductInfo) : LookupState
    data object NotFound : LookupState
    data class Failed(val message: String) : LookupState
}

data class FormErrors(
    val name: String? = null,
    val category: String? = null,
    val storage: String? = null,
    val dateType: String? = null,
    val printedDate: String? = null,
    val openedDate: String? = null,
    val useWithin: String? = null
) {
    val any: Boolean
        get() = listOf(name, category, storage, dateType, printedDate, openedDate, useWithin).any { it != null }
}

data class FoodFormState(
    val mode: FormMode = FormMode.MANUAL,
    val lookup: LookupState = LookupState.None,
    val loadingItem: Boolean = false,
    val notFound: Boolean = false,
    val barcode: String? = null,
    val brand: String? = null,
    val imageUrl: String? = null,
    val photoPath: String? = null,
    val name: String = "",
    val category: FoodCategory? = null,
    val storage: StorageLocation? = null,
    val dateType: DateType? = null,
    val printedDate: LocalDate? = null,
    val isOpened: Boolean = false,
    val openedDate: LocalDate? = null,
    val hasInstruction: Boolean = false,
    val useWithinText: String = "",
    val showErrors: Boolean = false,
    val saving: Boolean = false,
    val savedName: String? = null,
    val duplicates: Int = 0,
    val dirty: Boolean = false
) {
    val useWithinDays: Int? get() = useWithinText.toIntOrNull()

    // worked out every time rather than stored, so a message clears as soon as the field is fixed
    fun errors(today: LocalDate = LocalDate.now()): FormErrors {
        val opened = openedDate
        val days = useWithinDays
        return FormErrors(
            // the field caps typing at 60, this also catches a long name saved before that cap existed
            name = when {
                name.isBlank() -> "Enter a food name"
                name.trim().length > NAME_MAX -> "Keep the name to $NAME_MAX characters or fewer"
                else -> null
            },
            category = if (category == null) "Choose a category" else null,
            storage = if (storage == null) "Choose where it's stored" else null,
            dateType = if (dateType == null) "Choose the type of date on the pack" else null,
            // items that don't expire are the only ones allowed without a date
            printedDate = when {
                dateType?.needsDate == false || printedDate != null -> null
                dateType == DateType.OWN_DATE -> "Choose the day you'd like to use it by"
                else -> "A printed date is required"
            },
            openedDate = when {
                !isOpened -> null
                opened == null -> "Choose the day it was opened"
                opened.isAfter(today) -> "The opened date can't be in the future"
                else -> null
            },
            useWithin = when {
                !isOpened || !hasInstruction -> null
                days == null -> "Enter the number of days"
                days !in 1..Choices.USE_WITHIN_MAX -> "Enter 1 to ${Choices.USE_WITHIN_MAX} days"
                else -> null
            }
        )
    }

    // the live action date under the opened fields, null when there's nothing to show yet
    fun preview(): ActionInfo? {
        if (!isOpened) return null
        val printed = if (dateType == DateType.NO_EXPIRY) null else printedDate ?: return null
        return ActionDates.evaluate(printed, openedDate, if (hasInstruction) useWithinDays else null)
    }
}

// one form for all three routes in: typed by hand, prefilled from a barcode lookup, or editing a saved item
class FoodFormViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val pantry = PantryRepository(application)
    private val products = ProductRepository()
    private val photos = PhotoStore(application)
    private val itemId: String? = savedStateHandle.get<String>(Routes.ARG_ITEM_ID)
    private val barcode: String? = savedStateHandle.get<String>(Routes.ARG_BARCODE)
    private var original: PantryItem? = null

    private val _uiState = MutableStateFlow(
        FoodFormState(
            mode = when {
                itemId != null -> FormMode.EDIT
                barcode != null -> FormMode.LOOKUP
                else -> FormMode.MANUAL
            },
            barcode = barcode,
            loadingItem = itemId != null
        )
    )
    val uiState: StateFlow<FoodFormState> = _uiState.asStateFlow()

    init {
        if (itemId != null) {
            loadItem(itemId)
        } else if (barcode != null) {
            lookUp(barcode)
        }
    }

    private fun loadItem(id: String) {
        viewModelScope.launch {
            val item = pantry.getItem(id)?.takeIf { !it.deleted && it.outcome == null }
            original = item
            _uiState.update { state ->
                if (item == null) {
                    state.copy(loadingItem = false, notFound = true)
                } else {
                    state.copy(
                        loadingItem = false,
                        barcode = item.barcode,
                        brand = item.brand,
                        imageUrl = item.imageUrl,
                        photoPath = item.photoPath,
                        name = item.name,
                        category = item.category,
                        storage = item.storage,
                        dateType = item.dateType,
                        printedDate = item.printedDate,
                        isOpened = item.openedDate != null,
                        openedDate = item.openedDate,
                        hasInstruction = item.useWithinDays != null,
                        useWithinText = item.useWithinDays?.toString().orEmpty()
                    )
                }
            }
        }
    }

    fun retryLookup() {
        barcode?.let { lookUp(it) }
    }

    private fun lookUp(code: String) {
        _uiState.update { it.copy(lookup = LookupState.Loading) }
        viewModelScope.launch {
            val duplicates = pantry.countActiveWithBarcode(code)
            val result = products.lookup(code)
            _uiState.update { state ->
                when (result) {
                    // only fills what the user hasn't already typed while the lookup was running
                    is ProductLookup.Found -> state.copy(
                        lookup = LookupState.Found(result.product),
                        duplicates = duplicates,
                        brand = result.product.brand,
                        imageUrl = result.product.imageUrl,
                        name = state.name.ifBlank { result.product.name },
                        category = state.category ?: result.product.category,
                        storage = state.storage ?: result.product.category?.usualStorage
                    )
                    ProductLookup.NotFound -> state.copy(lookup = LookupState.NotFound, duplicates = duplicates)
                    is ProductLookup.Failed -> state.copy(
                        lookup = LookupState.Failed(result.message),
                        duplicates = duplicates
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = edit { it.copy(name = value) }

    // choosing a category suggests a storage place, but never overrides one already picked
    fun onCategoryChange(value: FoodCategory) = edit { it.copy(category = value, storage = it.storage ?: value.usualStorage) }

    fun onStorageChange(value: StorageLocation) = edit { it.copy(storage = value) }

    fun onDateTypeChange(value: DateType) = edit { it.copy(dateType = value) }

    fun onPrintedDateChange(value: LocalDate) = edit { it.copy(printedDate = value) }

    fun onPhotoChange(path: String?) = edit { it.copy(photoPath = path) }

    // a fresh file for the camera app, as a path to keep and a link the camera can write into
    fun newPhotoTarget(): Pair<String, Uri> {
        val file = photos.newCaptureFile()
        return file.absolutePath to photos.uriFor(file)
    }

    fun onPhotoTaken(path: String, saved: Boolean) {
        if (saved) onPhotoChange(path) else photos.delete(path)
    }

    // the picker's link only lasts a short while, so the image is copied into app storage first
    fun importPhoto(uri: Uri, onFailed: () -> Unit) {
        viewModelScope.launch {
            val path = photos.importFrom(uri)
            if (path != null) onPhotoChange(path) else onFailed()
        }
    }

    fun onOpenedChange(value: Boolean) = edit {
        it.copy(isOpened = value, openedDate = if (value) it.openedDate ?: LocalDate.now() else it.openedDate)
    }

    fun onOpenedDateChange(value: LocalDate) = edit { it.copy(openedDate = value) }

    fun onInstructionChange(value: Boolean) = edit { it.copy(hasInstruction = value) }

    fun onUseWithinChange(value: String) = edit { it.copy(useWithinText = value.filter { ch -> ch.isDigit() }.take(3)) }

    private fun edit(change: (FoodFormState) -> FoodFormState) {
        _uiState.update { change(it).copy(dirty = true) }
    }

    fun save() {
        val state = _uiState.value
        if (state.saving) return
        val category = state.category
        val storage = state.storage
        val dateType = state.dateType
        if (state.errors().any || category == null || storage == null || dateType == null) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        // a date picked before switching to "doesn't expire" is dropped rather than saved
        val printed = if (dateType.needsDate) state.printedDate else null
        val openedDate = if (state.isOpened) state.openedDate else null
        val useWithin = if (state.isOpened && state.hasInstruction) state.useWithinDays else null
        val base = original
        val item = base?.copy(
            name = state.name.trim(),
            photoPath = state.photoPath,
            category = category,
            storage = storage,
            dateType = dateType,
            printedDate = printed,
            openedDate = openedDate,
            useWithinDays = useWithin
        ) ?: PantryItem(
            name = state.name.trim(),
            brand = state.brand,
            barcode = state.barcode,
            imageUrl = state.imageUrl,
            photoPath = state.photoPath,
            category = category,
            storage = storage,
            dateType = dateType,
            printedDate = printed,
            openedDate = openedDate,
            useWithinDays = useWithin
        )
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            pantry.save(item)
            _uiState.update { it.copy(saving = false, savedName = item.name, dirty = false) }
        }
    }
}
