package com.example.shelfsense.screens.addfood

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shelfsense.data.model.DateType
import com.example.shelfsense.data.model.FoodCategory
import com.example.shelfsense.data.model.StorageLocation
import com.example.shelfsense.domain.DateDriver
import com.example.shelfsense.domain.DateExplainer
import com.example.shelfsense.domain.Dates
import com.example.shelfsense.ui.components.*
import com.example.shelfsense.ui.theme.ShelfTheme
import java.time.LocalDate

@Composable
fun FoodFormScreen(
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    viewModel: FoodFormViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = ShelfTheme.colors
    val messenger = LocalMessenger.current
    val focusManager = LocalFocusManager.current
    val today = remember { LocalDate.now() }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    val errors = state.errors(today)
    // errors only show after the first save attempt, so an empty form doesn't open covered in red
    val shown = if (state.showErrors) errors else FormErrors()
    val blocked = state.showErrors && errors.any

    LaunchedEffect(state.savedName) {
        val name = state.savedName ?: return@LaunchedEffect
        messenger.show(if (state.mode == FormMode.EDIT) "Changes to $name saved" else "$name added to your pantry")
        onSaved()
    }

    val requestClose: () -> Unit = {
        if (state.dirty) {
            confirmDiscard = true
        } else {
            onCancel()
        }
    }
    BackHandler(enabled = state.dirty) { confirmDiscard = true }

    val save: () -> Unit = {
        focusManager.clearFocus()
        viewModel.save()
    }
    val title = when (state.mode) {
        FormMode.EDIT -> "Edit food"
        FormMode.LOOKUP -> "Confirm details"
        FormMode.MANUAL -> "Add food"
    }

    Scaffold(containerColor = c.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        if (state.loadingItem) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.primary)
            }
            return@Scaffold
        }
        if (state.notFound) {
            Column(Modifier.padding(padding).padding(horizontal = 20.dp)) {
                ScreenHeader(title = title, onBack = onCancel)
                EmptyState(
                    icon = Icons.Filled.Inventory2,
                    title = "This item is no longer in your pantry",
                    body = "It may have been used up or deleted, possibly on another device.",
                    actionLabel = "Go back",
                    onAction = onCancel
                )
            }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            ScreenHeader(
                title = title,
                onBack = requestClose,
                actionLabel = "Save",
                onAction = save,
                actionEnabled = !state.saving && !blocked
            )
            LookupSection(state, onRetry = viewModel::retryLookup, onTryAnother = requestClose)
            if (state.duplicates > 0) {
                InfoBanner(
                    title = "Already in your pantry",
                    body = if (state.duplicates == 1) {
                        "You're already tracking one of these. Saving adds another."
                    } else {
                        "You're already tracking ${state.duplicates} of these. Saving adds another."
                    },
                    kind = BannerKind.INFO
                )
                Spacer(Modifier.height(16.dp))
            }
            if (blocked) {
                InfoBanner(title = "Check the highlighted fields", kind = BannerKind.ERROR)
                Spacer(Modifier.height(16.dp))
            }
            TextFieldRow(
                label = "Food name",
                value = state.name,
                onValueChange = viewModel::onNameChange,
                placeholder = "Add a name for this item",
                error = shown.name,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() }
            )
            PhotoSection(
                state = state,
                onNewTarget = viewModel::newPhotoTarget,
                onTaken = viewModel::onPhotoTaken,
                onPicked = { uri ->
                    viewModel.importPhoto(uri) { messenger.show("That photo couldn't be added. Try another one.") }
                },
                onRemove = { viewModel.onPhotoChange(null) }
            )
            DropdownField(
                label = "Category",
                options = FoodCategory.entries,
                selected = state.category,
                onSelect = viewModel::onCategoryChange,
                optionLabel = { it.label },
                placeholder = "Select a category",
                error = shown.category,
                optionIcon = { it.icon() }
            )
            DropdownField(
                label = "Storage location",
                options = StorageLocation.entries,
                selected = state.storage,
                onSelect = viewModel::onStorageChange,
                optionLabel = { it.label },
                placeholder = "Select a location",
                error = shown.storage,
                optionIcon = { it.icon() }
            )
            DropdownField(
                label = "Date type",
                options = DateType.entries,
                selected = state.dateType,
                onSelect = viewModel::onDateTypeChange,
                optionLabel = { it.label },
                placeholder = "Use by, best before or no date",
                error = shown.dateType,
                helper = dateTypeHelper(state.dateType)
            )
            // hidden for food that doesn't expire, since there's no date to enter
            AnimatedVisibility(visible = state.dateType?.needsDate != false) {
                Column {
                    DateField(
                        label = state.dateType?.dateLabel ?: "Printed date",
                        value = state.printedDate,
                        onPicked = viewModel::onPrintedDateChange,
                        error = shown.printedDate,
                        helper = printedHelper(state, today),
                        earliest = today.minusYears(1),
                        latest = today.plusYears(10)
                    )
                    QuickDateRow(
                        options = listOf(
                            "In 3 days" to today.plusDays(3),
                            "In a week" to today.plusWeeks(1),
                            "In 2 weeks" to today.plusWeeks(2),
                            "In a month" to today.plusMonths(1)
                        ),
                        selected = state.printedDate,
                        onPick = viewModel::onPrintedDateChange
                    )
                }
            }
            SectionLabel("Has this been opened?")
            Segmented(
                options = listOf("No", "Yes"),
                selectedIndex = if (state.isOpened) 1 else 0,
                onSelect = { viewModel.onOpenedChange(it == 1) }
            )
            AnimatedVisibility(visible = state.isOpened) {
                OpenedSection(
                    state = state,
                    errors = shown,
                    today = today,
                    onOpenedDate = viewModel::onOpenedDateChange,
                    onInstruction = viewModel::onInstructionChange,
                    onUseWithin = viewModel::onUseWithinChange
                )
            }
            ActionPreview(state)
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                if (state.mode == FormMode.EDIT) "Save changes" else "Save item",
                onClick = save,
                enabled = !blocked,
                loading = state.saving
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Cancel", onClick = requestClose)
            Spacer(Modifier.height(40.dp))
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(if (state.mode == FormMode.EDIT) "Discard your changes?" else "Discard this item?") },
            text = { Text("What you've entered so far won't be saved.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onCancel()
                }) { Text("Discard", color = c.urgent) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") }
            },
            containerColor = c.surface
        )
    }
}

private fun dateTypeHelper(dateType: DateType?): String = when (dateType) {
    DateType.OWN_DATE -> "For fresh food with nothing printed, you pick the date"
    DateType.NO_EXPIRY -> "For honey, salt, spirits and other food that keeps indefinitely"
    else -> "Use by is about safety, best before is about quality"
}

private fun printedHelper(state: FoodFormState, today: LocalDate): String? {
    val printed = state.printedDate
    return when {
        printed != null && printed.isBefore(today) -> "This date has already passed"
        state.dateType == DateType.OWN_DATE -> "Nothing on the pack, so pick when you'd like to use it by"
        printed == null && state.dateType == null &&
            (state.category == FoodCategory.FRUIT_VEG || state.category == FoodCategory.LEFTOVERS) ->
            "No date on the pack? Choose that option under Date type"
        else -> null
    }
}

@Composable
private fun LookupSection(state: FoodFormState, onRetry: () -> Unit, onTryAnother: () -> Unit) {
    val c = ShelfTheme.colors
    val code = state.barcode.orEmpty()
    when (val lookup = state.lookup) {
        LookupState.None -> Unit
        LookupState.Loading -> {
            ShelfCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = c.primary, strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Looking up $code", style = MaterialTheme.typography.labelLarge, color = c.ink)
                        Text("Checking Open Food Facts…", style = MaterialTheme.typography.labelSmall, color = c.muted)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        is LookupState.Found -> {
            InfoBanner(
                title = "Product found in Open Food Facts",
                body = if (lookup.product.name.isBlank()) "Some details are missing, so add a name below." else null,
                kind = BannerKind.SUCCESS
            )
            Spacer(Modifier.height(16.dp))
            ShelfCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FoodThumb(
                        name = lookup.product.name,
                        category = state.category ?: lookup.product.category ?: FoodCategory.DRY_GOODS,
                        imageUrl = lookup.product.imageUrl,
                        size = 64.dp,
                        storage = state.storage
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            lookup.product.name.ifBlank { "Unnamed product" },
                            style = MaterialTheme.typography.titleMedium,
                            color = c.ink
                        )
                        lookup.product.brand?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = c.muted)
                        }
                        Text(
                            "Barcode ${lookup.product.barcode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.muted
                        )
                    }
                }
            }
            // a misread barcode can still match a real product, so there's always a way back
            TextButton(onClick = onTryAnother) {
                Text(
                    "Not the right product? Try another barcode",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.primary
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        LookupState.NotFound -> {
            InfoBanner(
                title = "No match for $code",
                body = "Check that number matches the one printed under the barcode. Open Food Facts is built by the public, " +
                    "so some products are missing, but you can still add this one by hand below.",
                kind = BannerKind.WARNING,
                actionLabel = "Try another barcode",
                onAction = onTryAnother
            )
            Spacer(Modifier.height(16.dp))
        }
        is LookupState.Failed -> {
            InfoBanner(
                title = "The lookup didn't work",
                body = lookup.message,
                kind = BannerKind.ERROR,
                actionLabel = "Try again",
                onAction = onRetry
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// the launchers have to live in the UI, everything to do with storing the photo is in the ViewModel
@Composable
private fun PhotoSection(
    state: FoodFormState,
    onNewTarget: () -> Pair<String, Uri>,
    onTaken: (String, Boolean) -> Unit,
    onPicked: (Uri) -> Unit,
    onRemove: () -> Unit
) {
    val c = ShelfTheme.colors
    val messenger = LocalMessenger.current
    // kept across rotation, since the camera app can stay open for a while
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pendingCapture
        pendingCapture = null
        if (path != null) onTaken(path, saved)
    }
    // the system photo picker needs no storage permission
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri)
    }

    SectionLabel("Photo (optional)")
    Row(verticalAlignment = Alignment.CenterVertically) {
        FoodThumb(
            name = state.name,
            category = state.category ?: FoodCategory.DRY_GOODS,
            imageUrl = state.imageUrl,
            photoPath = state.photoPath,
            size = 64.dp,
            storage = state.storage
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    state.photoPath != null -> "Your photo"
                    state.imageUrl != null -> "Photo from Open Food Facts"
                    else -> "No photo yet"
                },
                style = MaterialTheme.typography.labelLarge,
                color = c.ink
            )
            Text(
                "Photos you take or choose stay on this phone.",
                style = MaterialTheme.typography.labelSmall,
                color = c.muted
            )
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PhotoChip("Take photo", Icons.Filled.PhotoCamera) {
            val (path, uri) = onNewTarget()
            pendingCapture = path
            try {
                takePicture.launch(uri)
            } catch (e: ActivityNotFoundException) {
                pendingCapture = null
                messenger.show("No camera app was found on this device.")
            }
        }
        PhotoChip("Choose photo", Icons.Filled.PhotoLibrary) {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        if (state.photoPath != null) {
            PhotoChip("Remove", Icons.Filled.Delete, onClick = onRemove)
        }
    }
}

@Composable
private fun PhotoChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    val c = ShelfTheme.colors
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        shape = RoundedCornerShape(16.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = c.surface,
            labelColor = c.ink,
            leadingIconContentColor = c.primary
        ),
        border = BorderStroke(1.dp, c.line)
    )
}

@Composable
private fun OpenedSection(
    state: FoodFormState,
    errors: FormErrors,
    today: LocalDate,
    onOpenedDate: (LocalDate) -> Unit,
    onInstruction: (Boolean) -> Unit,
    onUseWithin: (String) -> Unit
) {
    val c = ShelfTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        DateField(
            label = "Date opened",
            value = state.openedDate,
            onPicked = onOpenedDate,
            error = errors.openedDate,
            earliest = today.minusYears(1),
            latest = today
        )
        QuickDateRow(
            options = listOf(
                "Today" to today,
                "Yesterday" to today.minusDays(1),
                "2 days ago" to today.minusDays(2)
            ),
            selected = state.openedDate,
            onPick = onOpenedDate
        )
        SectionLabel("Does the package say how soon to use it?")
        Segmented(
            options = listOf("It does not say", "Use within"),
            selectedIndex = if (state.hasInstruction) 1 else 0,
            onSelect = { onInstruction(it == 1) }
        )
        if (state.hasInstruction) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                TextFieldRow(
                    label = "Days",
                    value = state.useWithinText,
                    onValueChange = onUseWithin,
                    modifier = Modifier.width(120.dp),
                    error = errors.useWithin,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    maxLength = 3
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "days after opening",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.ink,
                    modifier = Modifier.padding(top = 36.dp)
                )
            }
        }
        Text(
            "Enter the instruction shown on the packaging. ShelfSense never guesses it from the type of food.",
            style = MaterialTheme.typography.labelSmall,
            color = c.muted,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun ActionPreview(state: FoodFormState) {
    if (!state.isOpened) return
    val info = state.preview()
    // no date picked yet, so there's nothing to preview
    if (info == null && state.dateType != DateType.NO_EXPIRY) return
    val c = ShelfTheme.colors
    val opened = state.openedDate
    val days = state.useWithinDays
    val other = if (state.dateType == DateType.OWN_DATE) "the date you chose" else "the printed date"
    // "use within" is picked but the days box is still empty, so there's no date to show yet
    val waitingForDays = state.hasInstruction && days == null
    val note = when {
        waitingForDays -> "Enter the number of days from the pack to work out the action date."
        info == null -> "It doesn't expire and there's no after-opening instruction, so there's nothing to count down to."
        opened == null -> "Choose the day it was opened to see the action date."
        !state.hasInstruction || days == null -> "No after-opening instruction, so $other applies."
        state.dateType == DateType.NO_EXPIRY ->
            "Opened ${Dates.dayMonth(opened)} plus ${DateExplainer.plural(days, "day")}, from the instruction on the pack."
        info.driver == DateDriver.OPENING_DEADLINE ->
            "Opened ${Dates.dayMonth(opened)} plus ${DateExplainer.plural(days, "day")} comes before $other."
        info.driver == DateDriver.SAME_DAY -> "The opening deadline and $other fall on the same day."
        else -> "${other.replaceFirstChar { it.uppercase() }} comes before the opening deadline, so it still applies."
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(c.tint, RoundedCornerShape(14.dp))
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Text("SHELFSENSE ACTION DATE", style = MaterialTheme.typography.labelSmall, color = c.primary)
        if (!waitingForDays) {
            Spacer(Modifier.height(4.dp))
            Text(
                info?.let { Dates.long(it.actionDate) } ?: "None",
                style = MaterialTheme.typography.titleLarge,
                color = c.primary
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(note, style = MaterialTheme.typography.labelSmall, color = c.primary)
    }
}
