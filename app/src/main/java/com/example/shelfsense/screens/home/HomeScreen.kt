package com.example.shelfsense.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shelfsense.R
import com.example.shelfsense.data.model.StorageLocation
import com.example.shelfsense.domain.ActionDates
import com.example.shelfsense.domain.DateExplainer
import com.example.shelfsense.domain.Dates
import com.example.shelfsense.domain.OutcomeTotals
import com.example.shelfsense.domain.PantryFilter
import com.example.shelfsense.domain.TrackedItem
import com.example.shelfsense.ui.components.BannerKind
import com.example.shelfsense.ui.components.FoodThumb
import com.example.shelfsense.ui.components.InfoBanner
import com.example.shelfsense.ui.components.PermissionStep
import com.example.shelfsense.ui.components.PrimaryButton
import com.example.shelfsense.ui.components.ScreenHeader
import com.example.shelfsense.ui.components.SectionHeader
import com.example.shelfsense.ui.components.icon
import com.example.shelfsense.ui.components.locationColours
import com.example.shelfsense.ui.components.rememberNotificationAccess
import com.example.shelfsense.ui.components.urgencyStyle
import com.example.shelfsense.ui.theme.ShelfTheme
import java.time.LocalTime

// layout, sizes and colours follow the A1 home design
@Composable
fun HomeScreen(
    onOpenItem: (String) -> Unit,
    onOpenPantry: (PantryFilter) -> Unit,
    onOpenInsights: () -> Unit,
    onAddFood: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = ShelfTheme.colors
    val access = rememberNotificationAccess(state.notificationAsked, viewModel::markNotificationAsked)
    // only worth asking once there's something to be reminded about
    val showBanner = !state.loading && state.remindersEnabled && !state.bannerDismissed &&
        !access.allowed && state.totalActive > 0

    Scaffold(containerColor = c.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "header") {
                ScreenHeader {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${greeting()}, ${state.firstName ?: "there"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = c.muted
                    )
                    Text(
                        "Let's make the most\nof your food today.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.ink,
                        modifier = Modifier.semantics { heading() }
                    )
                }
            }
            if (showBanner) {
                item(key = "banner") {
                    InfoBanner(
                        title = "Get a heads-up before food needs using",
                        body = "Allow notifications and ShelfSense will remind you each day when items are close to their action date.",
                        kind = BannerKind.INFO,
                        actionLabel = if (access.step == PermissionStep.OPEN_SETTINGS) "Open settings" else "Allow",
                        onAction = access::resolve,
                        dismissLabel = "Not now",
                        onDismiss = viewModel::dismissBanner
                    )
                }
            }
            item(key = "locations") {
                LocationRow(state.counts, onOpenPantry)
            }
            item(key = "soon_header") {
                SectionHeader(
                    title = "Items expiring soon",
                    actionLabel = if (state.totalActive > 0) "See all" else null,
                    onAction = { onOpenPantry(PantryFilter.ALL) }
                )
            }
            when {
                state.loading -> Unit
                state.totalActive == 0 -> item(key = "empty") {
                    EmptyPantryCard(onAddFood)
                }
                state.dueSoon.isEmpty() -> item(key = "nothing_due") {
                    NothingDueCard(state.nextUp, onOpenItem)
                }
                else -> items(state.dueSoon, key = { it.item.id }) { tracked ->
                    UseFirstCard(tracked, onClick = { onOpenItem(tracked.item.id) })
                }
            }
            item(key = "impact") {
                ImpactCard(state.month, onOpenInsights, Modifier.padding(top = 6.dp))
            }
        }
    }
}

private fun greeting(hour: Int = LocalTime.now().hour): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

// each place keeps its own colour from the A1 design, fridge mint, pantry oat and freezer blue.
// the words still say which is which, so the colour is a shortcut rather than the only cue
@Composable
private fun LocationRow(counts: Map<StorageLocation, Int>, onOpen: (PantryFilter) -> Unit) {
    val c = ShelfTheme.colors
    val places = listOf(
        StorageLocation.FRIDGE to PantryFilter.FRIDGE,
        StorageLocation.PANTRY to PantryFilter.PANTRY,
        StorageLocation.FREEZER to PantryFilter.FREEZER
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        places.forEach { (location, filter) ->
            val colours = locationColours(location)
            val count = counts[location] ?: 0
            Column(
                Modifier
                    .weight(1f)
                    // a minimum rather than a fixed height, so larger font settings don't clip the text
                    .heightIn(min = 116.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colours.background)
                    .clickable(onClickLabel = "Show ${location.label.lowercase()} items") { onOpen(filter) }
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(location.icon(), contentDescription = null, tint = colours.accent, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text(location.label, style = MaterialTheme.typography.labelLarge, color = c.ink)
                Text(
                    if (count == 1) "1 item" else "$count items",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.ink
                )
            }
        }
    }
}

@Composable
private fun UseFirstCard(tracked: TrackedItem, onClick: () -> Unit) {
    val c = ShelfTheme.colors
    val item = tracked.item
    val accent = urgencyStyle(tracked.info?.urgency).fg
    val opened = item.openedDate
    Box(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface)
                .clickable(onClick = onClick)
                .padding(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoodThumb(item.name, item.category, item.imageUrl, size = 48.dp, photoPath = item.photoPath, storage = item.storage)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // the words carry the urgency, the colour only repeats it
                Text(
                    tracked.info?.let { ActionDates.daysLeftLabel(it.daysLeft) } ?: "No expiry",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent
                )
                if (opened != null) {
                    val days = item.useWithinDays
                    Text(
                        if (days != null) {
                            "Opened ${Dates.dayMonth(opened)}, use within ${DateExplainer.plural(days, "day")}"
                        } else {
                            "Opened ${Dates.dayMonth(opened)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = c.muted
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.muted)
        }
        // drawn over the card's edge rather than clipped by it, so the stripe keeps its straight ends
        Box(
            Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(accent, RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun NothingDueCard(nextUp: TrackedItem?, onOpenItem: (String) -> Unit) {
    val c = ShelfTheme.colors
    val info = nextUp?.info
    val tap = if (nextUp != null) {
        Modifier.clickable(onClickLabel = "Open ${nextUp.item.name}") { onOpenItem(nextUp.item.id) }
    } else {
        Modifier
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .then(tap)
            .padding(16.dp)
    ) {
        Text("Nothing due this week", style = MaterialTheme.typography.titleMedium, color = c.ink)
        Spacer(Modifier.height(4.dp))
        Text(
            if (nextUp != null && info != null) {
                "Next up is ${nextUp.item.name}, with ${ActionDates.daysLeftLabel(info.daysLeft).lowercase()}."
            } else {
                "Nothing in your pantry has a date to count down to."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = c.muted
        )
    }
}

@Composable
private fun EmptyPantryCard(onAddFood: () -> Unit) {
    val c = ShelfTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .padding(16.dp)
    ) {
        Text("Nothing in your pantry yet", style = MaterialTheme.typography.titleMedium, color = c.ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "Scan a barcode or add an item by hand to start tracking what you have.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.muted
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton("Add your first item", onClick = onAddFood)
    }
}

@Composable
private fun ImpactCard(totals: OutcomeTotals, onOpenInsights: () -> Unit, modifier: Modifier = Modifier) {
    val c = ShelfTheme.colors
    val (title, message) = impactCopy(totals)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            // mint across most of the card, only deepening towards the far corner
            .background(Brush.linearGradient(listOf(c.impactStart, c.impactStart, c.impactEnd)))
            .clickable(onClickLabel = "View insights", onClick = onOpenInsights)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // the all white leaves keep their contrast on the dark theme's gradient
        Image(
            painter = painterResource(if (c.isDark) R.drawable.logo1_dark else R.drawable.logo1),
            contentDescription = null,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = c.impactInk)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.labelSmall, color = c.impactInk)
            Text(
                "View insights",
                style = MaterialTheme.typography.labelLarge,
                color = c.impactInk,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.impactInk)
    }
}

// the copy shifts with how the month is going, rather than always cheering
private fun impactCopy(t: OutcomeTotals): Pair<String, String> = when {
    t.resolved == 0 ->
        "Track your impact" to
            "Mark items as consumed, donated or discarded and your progress this month shows up here."
    t.discarded == 0 ->
        "Nothing binned\nthis month!" to "${t.consumed} used and ${t.donated} donated so far this month."
    t.saved >= t.discarded ->
        "You're making\na difference!" to
            "${t.consumed} used and ${t.donated} donated this month, only ${t.discarded} binned."
    else ->
        "Room to improve\nthis month" to
            "${t.consumed} used and ${t.donated} donated, but ${t.discarded} binned. Insights has a tip."
}
