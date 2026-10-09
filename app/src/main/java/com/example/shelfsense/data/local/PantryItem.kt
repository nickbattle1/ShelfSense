package com.example.shelfsense.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.shelfsense.data.model.DateType
import com.example.shelfsense.data.model.FoodCategory
import com.example.shelfsense.data.model.Outcome
import com.example.shelfsense.data.model.StorageLocation
import com.example.shelfsense.domain.ActionDates
import com.example.shelfsense.domain.ActionInfo
import java.time.LocalDate
import java.util.UUID

// the single pantry table. items stay after they're used up so Insights can report on them,
// and outcome == null means the item is still in the pantry
@Entity(tableName = "pantry_items", indices = [Index("outcome"), Index("syncPending")])
data class PantryItem(
    // a uuid rather than an autoincrement id, so items can be created offline and reuse it as the Firestore document id
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val imageUrl: String? = null,
    // a photo taken or picked on this phone. it stays local, so it never goes to Firestore
    val photoPath: String? = null,
    val category: FoodCategory,
    val storage: StorageLocation,
    val dateType: DateType,
    // null only when the item doesn't expire
    val printedDate: LocalDate?,
    val openedDate: LocalDate? = null,
    val useWithinDays: Int? = null,
    // stored so Room can sort and filter on it, the repository recalculates it on every write.
    // null when there's nothing to count down to
    val actionDate: LocalDate? = printedDate,
    val outcome: Outcome? = null,
    val resolvedAt: Long? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncPending: Boolean = true,
    // a delete is kept as a tombstone until it has reached Firestore
    val deleted: Boolean = false
)

fun PantryItem.actionInfo(today: LocalDate = LocalDate.now()): ActionInfo? =
    ActionDates.evaluate(printedDate, openedDate, useWithinDays, today)
