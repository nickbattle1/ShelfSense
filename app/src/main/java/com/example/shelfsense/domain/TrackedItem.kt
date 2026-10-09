package com.example.shelfsense.domain

import com.example.shelfsense.data.local.PantryItem
import com.example.shelfsense.data.local.actionInfo
import java.time.LocalDate

// an item paired with its worked out action date, so lists only calculate it once per update
data class TrackedItem(val item: PantryItem, val info: ActionInfo)

fun List<PantryItem>.tracked(today: LocalDate = LocalDate.now()): List<TrackedItem> =
    map { TrackedItem(it, it.actionInfo(today)) }
