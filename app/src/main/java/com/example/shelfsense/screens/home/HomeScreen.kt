package com.example.shelfsense.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.shelfsense.ui.components.ShelfCard
import com.example.shelfsense.ui.components.UrgencyLabel
import com.example.shelfsense.ui.components.icon
import com.example.shelfsense.ui.components.rememberNotificationAccess
import com.example.shelfsense.ui.components.urgencyStyle
import com.example.shelfsense.ui.theme.ShelfTheme
import java.time.LocalTime

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
                    Spacer(Modifier.height(8.dp))
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
                    onAction = { onOpenPantry(if (state.soonCount > 0) PantryFilter.SOON else PantryFilter.ALL) }
                )
            }
            if (!state.loading && state.totalActive == 0) {
                item(key = "empty") {
                    EmptyPantryCard(onAddFood)
                }
            } else if (!state.loading && state.priority.isEmpty()) {
                item(key = "undated") {
                    UndatedCard()
                }
            }
            items(state.priority, key = { it.item.id }) { tracked ->
                UseFirstCard(tracked, onClick = { onOpenItem(tracked.item.id) })
            }
            item(key = "impact") {
                ImpactCard(state.month, onOpenInsights, Modifier.padding(top = 8.dp))
            }
        }
    }
}

private fun greeting(hour: Int = LocalTime.now().hour): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun LocationRow(counts: Map<StorageLocation, Int>, onOpen: (PantryFilter) -> Unit) {
    val c = ShelfTheme.colors
    val locations = listOf(
        StorageLocation.FRIDGE to PantryFilter.FRIDGE,
        StorageLocation.PANTRY to PantryFilter.PANTRY,
        StorageLocation.FREEZER to PantryFilter.FREEZER
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        locations.forEach { (location, filter) ->
            val count = counts[location] ?: 0
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surface)
                    .border(1.dp, c.line, RoundedCornerShape(14.dp))
                    .clickable(onClickLabel = "Show ${location.label.lowercase()} items") { onOpen(filter) }
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(40.dp).background(c.tint, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(location.icon(), contentDescription = null, tint = c.primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(location.label, style = MaterialTheme.typography.labelLarge, color = c.ink)
                Text(
                    if (count == 1) "1 item" else "$count items",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.muted
                )
            }
        }
    }
}

@Composable
private fun UseFirstCard(tracked: TrackedItem, onClick: () -> Unit) {
    val c = ShelfTheme.colors
    val item = tracked.item
    val style = urgencyStyle(tracked.info?.urgency)
    val opened = item.openedDate
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // the stripe repeats the urgency colour, the label beside it carries the meaning
        Box(Modifier.width(5.dp).fillMaxHeight().background(style.fg))
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 76.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoodThumb(item.name, item.category, item.imageUrl, photoPath = item.photoPath)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                UrgencyLabel(tracked.info, Modifier.padding(top = 2.dp))
                if (opened != null) {
                    val days = item.useWithinDays
                    Text(
                        if (days != null) {
                            "Opened ${Dates.short(opened)}, use within ${DateExplainer.plural(days, "day")}"
                        } else {
                            "Opened ${Dates.short(opened)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = c.muted
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.muted)
        }
    }
}

@Composable
private fun EmptyPantryCard(onAddFood: () -> Unit) {
    val c = ShelfTheme.colors
    ShelfCard {
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
private fun UndatedCard() {
    val c = ShelfTheme.colors
    ShelfCard {
        Text("Nothing to count down", style = MaterialTheme.typography.titleMedium, color = c.ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "Everything in your pantry is marked as not expiring, so there's nothing that needs using first.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.muted
        )
    }
}

@Composable
private fun ImpactCard(totals: OutcomeTotals, onOpenInsights: () -> Unit, modifier: Modifier = Modifier) {
    val c = ShelfTheme.colors
    val (title, message) = impactCopy(totals)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(c.impactStart, c.impactEnd)))
            .clickable(onClickLabel = "View insights", onClick = onOpenInsights)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = c.impactInk)
                Spacer(Modifier.height(6.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = c.impactInk)
            }
            // the all white leaves keep their contrast on the dark theme's gradient
            Image(
                painter = painterResource(if (c.isDark) R.drawable.logo1_dark else R.drawable.logo1),
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("View insights", style = MaterialTheme.typography.labelLarge, color = c.impactInk)
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = c.impactInk,
                modifier = Modifier.size(18.dp)
            )
        }
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
