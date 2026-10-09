package com.example.shelfsense.screens.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.shelfsense.domain.OutcomeTotals
import com.example.shelfsense.ui.theme.ShelfTheme

// the charts are drawn on a Canvas, so each one carries a text description for screen readers

@Composable
fun DonutChart(totals: OutcomeTotals, modifier: Modifier = Modifier) {
    val c = ShelfTheme.colors
    val progress = remember { Animatable(0f) }
    LaunchedEffect(totals) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700))
    }
    val segments = listOf(
        totals.consumed to c.consumed,
        totals.donated to c.donated,
        totals.discarded to c.discarded
    )
    val description = "${totals.consumed} consumed, ${totals.donated} donated, ${totals.discarded} discarded"
    Canvas(modifier.semantics { contentDescription = description }) {
        val stroke = size.minDimension * 0.16f
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        drawArc(
            color = c.surface.copy(alpha = 0.7f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke)
        )
        val total = totals.resolved
        if (total > 0) {
            var start = -90f
            segments.forEach { (value, colour) ->
                if (value > 0) {
                    val sweep = 360f * value / total * progress.value
                    drawArc(
                        color = colour,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke)
                    )
                    start += sweep
                }
            }
        }
    }
}

@Composable
fun OutcomeBars(totals: OutcomeTotals) {
    val c = ShelfTheme.colors
    val rows = listOf(
        Triple("Consumed", totals.consumed, c.consumed),
        Triple("Donated", totals.donated, c.donated),
        Triple("Discarded", totals.discarded, c.discarded)
    )
    val largest = rows.maxOf { it.second }.coerceAtLeast(1)
    rows.forEachIndexed { index, (label, value, colour) ->
        val fraction by animateFloatAsState(
            targetValue = value / largest.toFloat(),
            animationSpec = tween(700),
            label = label
        )
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = c.ink, modifier = Modifier.weight(1f))
            Text("$value", style = MaterialTheme.typography.labelLarge, color = c.ink)
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(11.dp)
                .background(c.chip, RoundedCornerShape(6.dp))
        ) {
            if (fraction > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(11.dp)
                        .background(colour, RoundedCornerShape(6.dp))
                )
            }
        }
        if (index < rows.lastIndex) Spacer(Modifier.height(12.dp))
    }
}

// six stacked columns, one per month, split by outcome
@Composable
fun MonthlyChart(months: List<MonthBar>, modifier: Modifier = Modifier) {
    val c = ShelfTheme.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = c.muted)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = c.ink)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(months) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(800))
    }
    val tallest = (months.maxOfOrNull { it.totals.resolved } ?: 0).coerceAtLeast(1)
    val description = "Outcomes by month. " + months.joinToString("; ") { m ->
        "${m.label}: ${m.totals.consumed} consumed, ${m.totals.donated} donated, ${m.totals.discarded} discarded"
    }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(190.dp)
            .semantics { contentDescription = description }
    ) {
        if (months.isEmpty()) return@Canvas
        val labelSpace = 22.dp.toPx()
        val valueSpace = 18.dp.toPx()
        val chartHeight = size.height - labelSpace - valueSpace
        val baseline = valueSpace + chartHeight
        val slot = size.width / months.size
        val barWidth = slot * 0.5f
        val corner = CornerRadius(6.dp.toPx())

        // faint guide lines at a quarter, a half and three quarters of the tallest month
        for (step in 1..3) {
            val y = baseline - chartHeight * step / 4f
            drawLine(c.line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        drawLine(c.line, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = 1.dp.toPx())

        months.forEachIndexed { index, month ->
            val totals = month.totals
            val left = slot * index + (slot - barWidth) / 2f
            if (totals.resolved > 0) {
                val stackHeight = chartHeight * totals.resolved / tallest * progress.value
                val top = baseline - stackHeight
                val outline = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = left,
                            top = top,
                            right = left + barWidth,
                            bottom = baseline,
                            topLeftCornerRadius = corner,
                            topRightCornerRadius = corner
                        )
                    )
                }
                clipPath(outline) {
                    var y = baseline
                    listOf(
                        totals.consumed to c.consumed,
                        totals.donated to c.donated,
                        totals.discarded to c.discarded
                    ).forEach { (value, colour) ->
                        val h = stackHeight * value / totals.resolved
                        drawRect(colour, topLeft = Offset(left, y - h), size = Size(barWidth, h))
                        y -= h
                    }
                }
                val valueText = measurer.measure(totals.resolved.toString(), style = valueStyle)
                drawText(
                    valueText,
                    topLeft = Offset(
                        left + (barWidth - valueText.size.width) / 2f,
                        top - valueText.size.height - 2.dp.toPx()
                    )
                )
            }
            val labelText = measurer.measure(month.label, style = labelStyle)
            drawText(
                labelText,
                topLeft = Offset(slot * index + (slot - labelText.size.width) / 2f, baseline + 4.dp.toPx())
            )
        }
    }
}

@Composable
fun ChartLegend() {
    val c = ShelfTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem("Consumed", c.consumed)
        LegendItem("Donated", c.donated)
        LegendItem("Discarded", c.discarded)
    }
}

@Composable
private fun LegendItem(label: String, colour: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(colour, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = ShelfTheme.colors.muted)
    }
}
