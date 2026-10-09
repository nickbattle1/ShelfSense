package com.example.shelfsense.data.remote

import com.example.shelfsense.data.local.PantryItem
import com.example.shelfsense.data.model.DateType
import com.example.shelfsense.data.model.FoodCategory
import com.example.shelfsense.data.model.Outcome
import com.example.shelfsense.data.model.StorageLocation
import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate

// converts between a Room row and the document at users/{uid}/pantry/{itemId}.
// dates go up as ISO strings so they read clearly in the Firebase console.
// photoPath is left out on purpose, it only means something on the phone that took the photo
object FirestoreMapper {

    fun toDocument(item: PantryItem): Map<String, Any?> = hashMapOf(
        "name" to item.name,
        "brand" to item.brand,
        "barcode" to item.barcode,
        "imageUrl" to item.imageUrl,
        "category" to item.category.name,
        "storage" to item.storage.name,
        "dateType" to item.dateType.name,
        "printedDate" to item.printedDate?.toString(),
        "openedDate" to item.openedDate?.toString(),
        "useWithinDays" to item.useWithinDays,
        "actionDate" to item.actionDate?.toString(),
        "outcome" to item.outcome?.name,
        "resolvedAt" to item.resolvedAt,
        "addedAt" to item.addedAt,
        "updatedAt" to item.updatedAt
    )

    // a malformed document is skipped rather than failing the whole pull.
    // the repository recalculates the action date before storing the result
    fun fromDocument(doc: DocumentSnapshot): PantryItem? = runCatching {
        val name = doc.getString("name") ?: return null
        val dateType = DateType.valueOf(doc.getString("dateType") ?: return null)
        val printed = doc.getString("printedDate")?.let { LocalDate.parse(it) }
        if (printed == null && dateType.needsDate) return null
        PantryItem(
            id = doc.id,
            name = name,
            brand = doc.getString("brand"),
            barcode = doc.getString("barcode"),
            imageUrl = doc.getString("imageUrl"),
            category = FoodCategory.valueOf(doc.getString("category") ?: return null),
            storage = StorageLocation.valueOf(doc.getString("storage") ?: return null),
            dateType = dateType,
            printedDate = printed,
            openedDate = doc.getString("openedDate")?.let { LocalDate.parse(it) },
            useWithinDays = doc.getLong("useWithinDays")?.toInt(),
            outcome = doc.getString("outcome")?.let { Outcome.valueOf(it) },
            resolvedAt = doc.getLong("resolvedAt"),
            addedAt = doc.getLong("addedAt") ?: System.currentTimeMillis(),
            updatedAt = doc.getLong("updatedAt") ?: 0L,
            syncPending = false
        )
    }.getOrNull()
}
