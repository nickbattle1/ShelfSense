package com.example.shelfsense.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shelfsense.data.local.PantryItem
import com.example.shelfsense.data.model.Choices
import com.example.shelfsense.data.model.DateType
import com.example.shelfsense.data.model.Outcome
import com.example.shelfsense.domain.ActionDates
import com.example.shelfsense.domain.ActionInfo
import com.example.shelfsense.domain.DateExplainer
import com.example.shelfsense.domain.Dates
import com.example.shelfsense.domain.StorageGuidance
import com.example.shelfsense.ui.components.*
import com.example.shelfsense.ui.theme.ShelfTheme
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: FoodDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = ShelfTheme.colors
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    var showWhy by rememberSaveable { mutableStateOf(false) }
    var showOpened by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val whySheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val openedSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val item = state.item
    // null for items with nothing to count down to
    val info = state.info

    // every outcome can be undone from the snackbar, which lives in the root scaffold
    val resolve: (Outcome) -> Unit = { outcome ->
        viewModel.recordOutcome(outcome) { snapshot ->
            messenger.show("${snapshot.name} marked as ${outcome.pastTense}", actionLabel = "Undo") {
                viewModel.undo(snapshot)
            }
            onBack()
        }
    }

    Scaffold(containerColor = c.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        if (state.loading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.primary)
            }
            return@Scaffold
        }
        if (item == null) {
            Column(Modifier.padding(padding).padding(horizontal = 20.dp)) {
                ScreenHeader(title = "Food details", onBack = onBack)
                EmptyState(
                    icon = Icons.Filled.Inventory2,
                    title = "This item is no longer in your pantry",
                    body = "It may have been used up or deleted, possibly on another device.",
                    actionLabel = "Go back",
                    onAction = onBack
                )
            }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            ScreenHeader(
                title = "Food details",
                onBack = onBack,
                actionLabel = "Edit",
                onAction = { onEdit(item.id) },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = c.ink)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (item.openedDate == null) {
                                DropdownMenuItem(
                                    text = { Text("Mark as opened") },
                                    leadingIcon = { Icon(Icons.Filled.LockOpen, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        showOpened = true
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete item", color = c.urgent) },
                                leadingIcon = {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = c.urgent)
                                },
                                onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                }
                            )
                        }
                    }
                }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                FoodThumb(item.name, item.category, item.imageUrl, size = 56.dp, photoPath = item.photoPath)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = c.ink,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        listOfNotNull(item.brand, item.category.label).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.muted
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            if (info != null) {
                UrgencyCard(item, info, onWhy = { showWhy = true })
            } else {
                NoDateCard()
            }

            if (item.openedDate != null && item.useWithinDays == null && item.dateType.needsDate) {
                Spacer(Modifier.height(12.dp))
                InfoBanner(
                    title = "No after-opening instruction",
                    body = "Check the package for how soon to use it once opened, then add it for a tighter date.",
                    kind = BannerKind.INFO,
                    actionLabel = "Add it",
                    onAction = { onEdit(item.id) }
                )
            }

            SectionLabel("Shelf life")
            ShelfCard {
                DetailRow(dateRowLabel(item.dateType), item.printedDate?.let { Dates.long(it) } ?: "None")
                CardDivider()
                if (item.openedDate != null) {
                    DetailRow("Opened", Dates.long(item.openedDate))
                } else {
                    DetailRow("Opened", "Not opened")
                    TextButton(onClick = { showOpened = true }, contentPadding = PaddingValues(0.dp)) {
                        Text("Mark as opened", style = MaterialTheme.typography.labelLarge, color = c.primary)
                    }
                }
                CardDivider()
                DetailRow(
                    "Package instruction",
                    item.useWithinDays?.let { "Use within ${DateExplainer.plural(it, "day")}" } ?: "Not recorded"
                )
                CardDivider()
                DetailRow(
                    "ShelfSense action date",
                    info?.let { Dates.long(it.actionDate) } ?: "None",
                    strong = true
                )
            }

            SectionLabel("Storage")
            ShelfCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(item.storage.icon(), contentDescription = null, tint = c.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(item.storage.label, style = MaterialTheme.typography.labelLarge, color = c.ink)
                }
                Spacer(Modifier.height(8.dp))
                Text(StorageGuidance.forLocation(item.storage), style = MaterialTheme.typography.bodyMedium, color = c.ink)
                Spacer(Modifier.height(6.dp))
                Text(StorageGuidance.forCategory(item.category), style = MaterialTheme.typography.bodyMedium, color = c.ink)
                Spacer(Modifier.height(10.dp))
                Text(StorageGuidance.PACKAGE_FIRST, style = MaterialTheme.typography.labelSmall, color = c.muted)
            }

            val added = "Added ${Dates.short(Dates.toLocalDate(item.addedAt))}"
            Text(
                if (item.barcode != null) "Barcode ${item.barcode} · $added" else added,
                style = MaterialTheme.typography.labelSmall,
                color = c.muted,
                modifier = Modifier.padding(top = 10.dp)
            )

            SectionLabel("What happened to it?")
            PrimaryButton("Consumed", onClick = { resolve(Outcome.CONSUMED) })
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Donated", onClick = { resolve(Outcome.DONATED) }, modifier = Modifier.weight(1f))
                SecondaryButton(
                    "Discarded",
                    onClick = { resolve(Outcome.DISCARDED) },
                    modifier = Modifier.weight(1f),
                    contentColor = c.urgent
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showWhy && item != null && info != null) {
        ModalBottomSheet(
            onDismissRequest = { showWhy = false },
            sheetState = whySheet,
            containerColor = c.surface
        ) {
            WhyThisDate(item, info, onClose = {
                scope.launch { whySheet.hide() }.invokeOnCompletion { showWhy = false }
            })
        }
    }

    if (showOpened && item != null) {
        ModalBottomSheet(
            onDismissRequest = { showOpened = false },
            sheetState = openedSheet,
            containerColor = c.surface
        ) {
            MarkOpenedSheet(
                item = item,
                onSave = { date, days ->
                    viewModel.markOpened(date, days)
                    scope.launch { openedSheet.hide() }.invokeOnCompletion { showOpened = false }
                    messenger.show("${item.name} marked as opened")
                },
                onCancel = {
                    scope.launch { openedSheet.hide() }.invokeOnCompletion { showOpened = false }
                }
            )
        }
    }

    if (confirmDelete && item != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = c.urgent) },
            title = { Text("Delete ${item.name}?") },
            text = {
                Text(
                    "It will be removed from your pantry and won't count towards your insights. " +
                        "If you threw it out, choose Discarded instead so your report stays accurate."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete { snapshot ->
                        messenger.show("${snapshot.name} deleted", actionLabel = "Undo") { viewModel.undo(snapshot) }
                        onBack()
                    }
                }) { Text("Delete", color = c.urgent) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
            containerColor = c.surface
        )
    }
}

private fun dateRowLabel(dateType: DateType): String = when (dateType) {
    DateType.USE_BY -> "Printed use by"
    DateType.BEST_BEFORE -> "Printed best before"
    DateType.OWN_DATE -> "Your use-by date"
    DateType.NO_EXPIRY -> "Date on the pack"
}

@Composable
private fun UrgencyCard(item: PantryItem, info: ActionInfo, onWhy: () -> Unit) {
    val style = urgencyStyle(info.urgency)
    val advice = DateExplainer.overdueAdvice(info, item.dateType, item.useWithinDays)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(style.bg)
            .border(1.dp, style.fg.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(style.icon, contentDescription = null, tint = style.fg, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    DateExplainer.headline(info, item.dateType),
                    style = MaterialTheme.typography.titleMedium,
                    color = style.fg
                )
                Text(
                    "Action date ${Dates.long(info.actionDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = style.fg
                )
                if (advice != null) {
                    Text(
                        advice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = style.fg,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = style.fg.copy(alpha = 0.25f))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Explain this date", onClick = onWhy)
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = style.fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Why this date?", style = MaterialTheme.typography.labelLarge, color = style.fg, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = style.fg)
        }
    }
}

// shown instead of the urgency card when there's nothing to count down to
@Composable
private fun NoDateCard() {
    val c = ShelfTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.tint, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Filled.AllInclusive, contentDescription = null, tint = c.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Doesn't expire", style = MaterialTheme.typography.titleMedium, color = c.primary)
            Text(
                "There's no date to count down to, so it stays out of reminders. Mark it used once it runs out.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.primary
            )
        }
    }
}

@Composable
private fun WhyThisDate(item: PantryItem, info: ActionInfo, onClose: () -> Unit) {
    val c = ShelfTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            "Why this date?",
            style = MaterialTheme.typography.titleLarge,
            color = c.ink,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(12.dp))
        DateExplainer.reasons(item.printedDate, item.dateType, item.openedDate, item.useWithinDays, info)
            .forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = Modifier.padding(bottom = 8.dp))
            }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(c.tint, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Text("ACTION DATE", style = MaterialTheme.typography.labelSmall, color = c.primary)
            Text(Dates.long(info.actionDate), style = MaterialTheme.typography.titleLarge, color = c.primary)
        }
        Spacer(Modifier.height(14.dp))
        Text(DateExplainer.dateTypeNote(item.dateType), style = MaterialTheme.typography.bodyMedium, color = c.muted)
        Spacer(Modifier.height(8.dp))
        Text(StorageGuidance.PACKAGE_FIRST, style = MaterialTheme.typography.labelSmall, color = c.muted)
        Spacer(Modifier.height(20.dp))
        SecondaryButton("Close", onClick = onClose)
    }
}

@Composable
private fun MarkOpenedSheet(item: PantryItem, onSave: (LocalDate, Int?) -> Unit, onCancel: () -> Unit) {
    val c = ShelfTheme.colors
    val today = remember { LocalDate.now() }
    var opened by rememberSaveable { mutableStateOf(today) }
    var hasInstruction by rememberSaveable { mutableStateOf(false) }
    var daysText by rememberSaveable { mutableStateOf("") }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val days = daysText.toIntOrNull()
    val daysError = when {
        !hasInstruction -> null
        days == null -> "Enter the number of days"
        days !in 1..Choices.USE_WITHIN_MAX -> "Enter 1 to ${Choices.USE_WITHIN_MAX} days"
        else -> null
    }
    val preview = ActionDates.evaluate(item.printedDate, opened, if (hasInstruction) days else null)

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            "Mark as opened",
            style = MaterialTheme.typography.titleLarge,
            color = c.ink,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Record when you opened ${item.name} and what the package says about using it after opening.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.muted
        )
        Spacer(Modifier.height(16.dp))
        DateField(
            label = "Date opened",
            value = opened,
            onPicked = { opened = it },
            earliest = today.minusYears(1),
            latest = today
        )
        QuickDateRow(
            options = listOf("Today" to today, "Yesterday" to today.minusDays(1), "2 days ago" to today.minusDays(2)),
            selected = opened,
            onPick = { opened = it }
        )
        SectionLabel("Does the package say how soon to use it?")
        Segmented(
            options = listOf("It does not say", "Use within"),
            selectedIndex = if (hasInstruction) 1 else 0,
            onSelect = { hasInstruction = it == 1 }
        )
        if (hasInstruction) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                TextFieldRow(
                    label = "Days",
                    value = daysText,
                    onValueChange = { daysText = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier.width(120.dp),
                    placeholder = "5",
                    error = if (showErrors) daysError else null,
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
        Spacer(Modifier.height(12.dp))
        Text(
            preview?.let { "New action date: ${Dates.long(it.actionDate)}" }
                ?: "No action date, since it doesn't expire",
            style = MaterialTheme.typography.labelLarge,
            color = c.primary
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Save", onClick = {
            showErrors = true
            if (daysError == null) onSave(opened, if (hasInstruction) days else null)
        })
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Cancel", onClick = onCancel)
    }
}
