package com.example.shelfsense.screens.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelfsense.data.local.CategoryCount
import com.example.shelfsense.data.repository.PantryRepository
import com.example.shelfsense.domain.Dates
import com.example.shelfsense.domain.OutcomeTotals
import com.example.shelfsense.domain.toTotals
import com.example.shelfsense.domain.totalsOf
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class ReportPeriod(val days: Long, val label: String, val comparisonLabel: String) {
    WEEK(7, "Last 7 days", "the previous 7 days"),
    MONTH(30, "Last 30 days", "the previous 30 days"),
    QUARTER(90, "Last 3 months", "the previous 3 months"),
    YEAR(365, "Last 12 months", "the previous 12 months")
}

data class MonthBar(val label: String, val totals: OutcomeTotals)

data class InsightsUiState(
    val loading: Boolean = true,
    val period: ReportPeriod = ReportPeriod.MONTH,
    val current: OutcomeTotals = OutcomeTotals(),
    val previous: OutcomeTotals = OutcomeTotals(),
    val months: List<MonthBar> = emptyList(),
    val mostDiscarded: CategoryCount? = null
)

// every figure on the screen comes from GROUP BY queries over the pantry table in Room
@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val pantry = PantryRepository(application)
    private val period = MutableStateFlow(ReportPeriod.MONTH)

    val uiState: StateFlow<InsightsUiState> = period.flatMapLatest { selected ->
        val now = System.currentTimeMillis()
        val from = now - selected.days * DAY_MS
        val previousFrom = from - selected.days * DAY_MS
        val firstMonth = YearMonth.now().minusMonths(MONTHS - 1)
        combine(
            // open ended, so anything resolved while the screen is open still counts
            pantry.observeOutcomeCounts(from, Long.MAX_VALUE),
            pantry.observeOutcomeCounts(previousFrom, from),
            pantry.observeMonthlyOutcomes(Dates.startOfDayMillis(firstMonth.atDay(1))),
            pantry.observeMostDiscarded(from, Long.MAX_VALUE)
        ) { current, previous, monthly, top ->
            InsightsUiState(
                loading = false,
                period = selected,
                current = current.toTotals(),
                previous = previous.toTotals(),
                months = (0 until MONTHS).map { offset ->
                    val month = firstMonth.plusMonths(offset)
                    // YearMonth prints as yyyy-MM, the same key the SQL groups by
                    val key = month.toString()
                    MonthBar(
                        label = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        totals = totalsOf(monthly.filter { it.month == key }.map { it.outcome to it.count })
                    )
                },
                mostDiscarded = top
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    fun selectPeriod(value: ReportPeriod) {
        period.value = value
    }

    private companion object {
        const val DAY_MS = 86_400_000L
        const val MONTHS = 6L
    }
}
