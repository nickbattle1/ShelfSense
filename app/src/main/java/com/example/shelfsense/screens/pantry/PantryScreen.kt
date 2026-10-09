package com.example.shelfsense.screens.pantry

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shelfsense.domain.Dates
import com.example.shelfsense.domain.PantryFilter
import com.example.shelfsense.domain.TrackedItem
import com.example.shelfsense.ui.components.ChoiceChip
import com.example.shelfsense.ui.components.EmptyState
import com.example.shelfsense.ui.components.FoodThumb
import com.example.shelfsense.ui.components.ScreenHeader
import com.example.shelfsense.ui.components.UrgencyLabel
import com.example.shelfsense.ui.theme.ShelfTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(
    onOpenItem: (String) -> Unit,
    onAddFood: () -> Unit,
    viewModel: PantryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = ShelfTheme.colors
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(containerColor = c.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Box(Modifier.padding(horizontal = 20.dp)) {
                ScreenHeader(title = "Pantry")
            }
            // docked and never expanded: results filter the list underneath as you type
            DockedSearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = state.query,
                        onQueryChange = viewModel::onQueryChange,
                        onSearch = { keyboard?.hide() },
                        expanded = false,
                        onExpandedChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search your pantry", color = c.muted) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = c.muted) },
                        trailingIcon = if (state.query.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = c.muted)
                                }
                            }
                        } else {
                            null
                        },
                        colors = SearchBarDefaults.inputFieldColors(
                            focusedTextColor = c.ink,
                            unfocusedTextColor = c.ink,
                            focusedContainerColor = c.surface,
                            unfocusedContainerColor = c.surface,
                            cursorColor = c.primary
                        )
                    )
                },
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(1.dp, c.line, SearchBarDefaults.inputFieldShape),
                shape = SearchBarDefaults.inputFieldShape,
                colors = SearchBarDefaults.colors(containerColor = c.surface, dividerColor = c.line),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) { }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PantryFilter.entries) { option ->
                    ChoiceChip(option.label, state.filter == option) { viewModel.onFilterChange(option) }
                }
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.primary)
                }
                state.totalActive == 0 -> EmptyState(
                    icon = Icons.Filled.Kitchen,
                    title = "Nothing in your pantry yet",
                    body = "Scan a barcode or add an item by hand to start tracking what you have.",
                    actionLabel = "Add your first item",
                    onAction = onAddFood
                )
                state.items.isEmpty() -> EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "No matches",
                    body = if (state.query.isNotBlank()) {
                        "Nothing matches \"${state.query.trim()}\". Try another name, or clear the search and filters."
                    } else {
                        "Nothing fits the ${state.filter.label.lowercase()} filter right now."
                    },
                    actionLabel = "Clear search and filters",
                    onAction = viewModel::clearSearch
                )
                else -> {
                    val filtered = state.items.size != state.totalActive
                    Text(
                        if (filtered) {
                            "${state.items.size} of ${state.totalActive} items, soonest first"
                        } else {
                            "${state.totalActive} ${if (state.totalActive == 1) "item" else "items"}, soonest first"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = c.muted,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
                    ) {
                        items(state.items, key = { it.item.id }) { tracked ->
                            PantryRow(
                                tracked = tracked,
                                onClick = { onOpenItem(tracked.item.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PantryRow(tracked: TrackedItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = ShelfTheme.colors
    val item = tracked.item
    val details = buildString {
        append(item.category.label)
        append(" · ")
        append(item.storage.label)
        if (item.openedDate != null) append(" · Opened")
    }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoodThumb(item.name, item.category, item.imageUrl, size = 44.dp, photoPath = item.photoPath)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    details,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                UrgencyLabel(tracked.info)
                Text(
                    tracked.info?.let { Dates.short(it.actionDate) } ?: "No date",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.muted
                )
            }
        }
        HorizontalDivider(Modifier.padding(start = 76.dp, end = 20.dp), color = c.line)
    }
}
