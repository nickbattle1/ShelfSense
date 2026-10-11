package com.example.shelfsense.domain

import com.example.shelfsense.data.local.OutcomeCount
import com.example.shelfsense.data.model.Outcome
import kotlin.math.roundToInt

// totals behind the Home impact card and the Insights report
data class OutcomeTotals(val consumed: Int = 0, val donated: Int = 0, val discarded: Int = 0) {
    val resolved: Int get() = consumed + donated + discarded
    val saved: Int get() = consumed + donated

    // share of finished items that were eaten or given away rather than binned
    val avoidanceRate: Int? get() = if (resolved == 0) null else (saved * 100f / resolved).roundToInt()
}

fun totalsOf(counts: List<Pair<Outcome, Int>>): OutcomeTotals = OutcomeTotals(
    consumed = counts.filter { it.first == Outcome.CONSUMED }.sumOf { it.second },
    donated = counts.filter { it.first == Outcome.DONATED }.sumOf { it.second },
    discarded = counts.filter { it.first == Outcome.DISCARDED }.sumOf { it.second }
)

fun List<OutcomeCount>.toTotals(): OutcomeTotals = totalsOf(map { it.outcome to it.count })

// how the avoidance rate reads on Insights. the band is always named in words beside its colour
enum class RateBand(val label: String) {
    GOOD("On track"),
    FAIR("Getting there"),
    LOW("Room to improve")
}

fun rateBand(rate: Int): RateBand = when {
    rate >= 80 -> RateBand.GOOD
    rate >= 60 -> RateBand.FAIR
    else -> RateBand.LOW
}
