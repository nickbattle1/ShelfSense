package com.example.shelfsense.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shelfsense.data.local.CategoryCount
import com.example.shelfsense.domain.RateBand
import com.example.shelfsense.domain.StorageGuidance
import com.example.shelfsense.domain.rateBand
import com.example.shelfsense.ui.components.CardDivider
import com.example.shelfsense.ui.components.DropdownField
import com.example.shelfsense.ui.components.EmptyState
import com.example.shelfsense.ui.components.ScreenHeader
import com.example.shelfsense.ui.components.SectionLabel
import com.example.shelfsense.ui.components.ShelfCard
import com.example.shelfsense.ui.components.icon
import com.example.shelfsense.ui.theme.ShelfTheme

@Composable
fun InsightsScreen(viewModel: InsightsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = ShelfTheme.colors
    val nothingYet = !state.loading && state.current.resolved == 0 && state.months.all { it.totals.resolved == 0 }

    Scaffold(containerColor = c.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            ScreenHeader(title = "Insights")
            DropdownField(
                label = "Reporting period",
                options = ReportPeriod.entries,
                selected = state.period,
                onSelect = viewModel::selectPeriod,
                optionLabel = { it.label },
                modifier = Modifier.fillMaxWidth(0.62f)
            )
            if (nothingYet) {
                EmptyState(
                    icon = Icons.Filled.Insights,
                    title = "No outcomes yet",
                    body = "When you mark items as consumed, donated or discarded, your waste report builds up here."
                )
            } else {
                RateCard(state)
                SectionLabel("Outcomes")
                ShelfCard { OutcomeBars(state.current) }
                SectionLabel("Six month trend")
                ShelfCard {
                    MonthlyChart(state.months)
                    Spacer(Modifier.height(10.dp))
                    ChartLegend()
                    CardDivider()
                    ComparisonLine(state)
                }
                SectionLabel("Most discarded")
                ShelfCard { MostDiscarded(state.mostDiscarded) }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun RateCard(state: InsightsUiState) {
    val c = ShelfTheme.colors
    val totals = state.current
    val rate = totals.avoidanceRate
    val band = rate?.let { rateBand(it) }
    // green on green passes contrast in dark mode but reads flat, so the words switch to the light text colour there
    val text = if (c.isDark) c.ink else c.primary
    // the percentage takes its band's colour, and the sentence below names the band, so colour is never the only cue
    val rateColour = when (band) {
        RateBand.GOOD -> c.primary
        RateBand.FAIR -> c.warn
        RateBand.LOW -> c.urgent
        null -> text
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.tint, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("WASTE AVOIDANCE RATE", style = MaterialTheme.typography.labelSmall, color = text)
            Text(
                if (rate != null) "$rate%" else "No data",
                style = MaterialTheme.typography.headlineMedium,
                color = rateColour
            )
            Text(
                if (rate != null && band != null) {
                    "${band.label}. ${totals.saved} of ${totals.resolved} resolved items were consumed or donated."
                } else {
                    "No outcomes recorded in the ${state.period.label.lowercase()}."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = text
            )
        }
        Spacer(Modifier.width(12.dp))
        DonutChart(totals, Modifier.size(88.dp))
    }
}

@Composable
private fun ComparisonLine(state: InsightsUiState) {
    val c = ShelfTheme.colors
    val now = state.current.avoidanceRate
    val before = state.previous.avoidanceRate
    val comparison = state.period.comparisonLabel
    val (icon, text) = when {
        now == null -> Icons.AutoMirrored.Filled.TrendingFlat to "Nothing to compare yet in the ${state.period.label.lowercase()}."
        before == null -> Icons.AutoMirrored.Filled.TrendingFlat to "Not enough history to compare with $comparison yet."
        now > before -> Icons.AutoMirrored.Filled.TrendingUp to
            "Improved by ${now - before} percentage points on $comparison ($before% to $now%)."
        now < before -> Icons.AutoMirrored.Filled.TrendingDown to
            "Down ${before - now} percentage points on $comparison ($before% to $now%)."
        else -> Icons.AutoMirrored.Filled.TrendingFlat to "Level with $comparison at $now%."
    }
    val tint = when {
        now != null && before != null && now > before -> c.primary
        now != null && before != null && now < before -> c.urgent
        else -> c.muted
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.ink)
    }
}

@Composable
private fun MostDiscarded(top: CategoryCount?) {
    val c = ShelfTheme.colors
    if (top == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = c.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Nothing binned in this period. Nice work.", style = MaterialTheme.typography.bodyMedium, color = c.ink)
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(c.urgentBg, CircleShape), contentAlignment = Alignment.Center) {
            Icon(top.category.icon(), contentDescription = null, tint = c.urgent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(top.category.label, style = MaterialTheme.typography.titleMedium, color = c.ink)
            Text(
                if (top.count == 1) "1 item binned this period" else "${top.count} items binned this period",
                style = MaterialTheme.typography.bodyMedium,
                color = c.muted
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(StorageGuidance.wasteTip(top.category), style = MaterialTheme.typography.bodyMedium, color = c.ink)
}
