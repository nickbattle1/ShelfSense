package com.example.shelfsense.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryItemDao {

    // active means still in the pantry: no outcome yet and not deleted
    @Query(
        "SELECT * FROM pantry_items WHERE outcome IS NULL AND deleted = 0 " +
            "ORDER BY actionDate ASC, name COLLATE NOCASE ASC"
    )
    fun observeActive(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE id = :id AND deleted = 0")
    fun observeById(id: String): Flow<PantryItem?>

    @Query("SELECT * FROM pantry_items WHERE id = :id")
    suspend fun getById(id: String): PantryItem?

    @Query(
        "SELECT * FROM pantry_items WHERE outcome IS NULL AND deleted = 0 AND actionDate <= :lastDay " +
            "ORDER BY actionDate ASC"
    )
    suspend fun getActiveDueBy(lastDay: LocalDate): List<PantryItem>

    @Query("SELECT COUNT(*) FROM pantry_items WHERE barcode = :barcode AND outcome IS NULL AND deleted = 0")
    suspend fun countActiveWithBarcode(barcode: String): Int

    @Upsert
    suspend fun upsert(item: PantryItem)

    @Upsert
    suspend fun upsertAll(items: List<PantryItem>)

    @Query("SELECT * FROM pantry_items WHERE syncPending = 1")
    suspend fun getPendingSync(): List<PantryItem>

    @Query("SELECT COUNT(*) FROM pantry_items WHERE syncPending = 1")
    fun observePendingCount(): Flow<Int>

    // the updatedAt check means an edit made during the upload stays pending for the next run
    @Query("UPDATE pantry_items SET syncPending = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query("DELETE FROM pantry_items WHERE id = :id AND deleted = 1 AND updatedAt = :updatedAt")
    suspend fun purgeDeleted(id: String, updatedAt: Long)

    @Query("DELETE FROM pantry_items")
    suspend fun clearAll()

    // report queries. resolvedAt is epoch millis and the upper bound is exclusive
    @Query(
        "SELECT outcome, COUNT(*) AS count FROM pantry_items " +
            "WHERE outcome IS NOT NULL AND deleted = 0 AND resolvedAt >= :from AND resolvedAt < :to " +
            "GROUP BY outcome"
    )
    fun observeOutcomeCounts(from: Long, to: Long): Flow<List<OutcomeCount>>

    // grouped by calendar month in the phone's own time zone
    @Query(
        "SELECT strftime('%Y-%m', resolvedAt / 1000, 'unixepoch', 'localtime') AS month, outcome, COUNT(*) AS count " +
            "FROM pantry_items WHERE outcome IS NOT NULL AND deleted = 0 AND resolvedAt >= :from " +
            "GROUP BY month, outcome ORDER BY month"
    )
    fun observeMonthlyOutcomes(from: Long): Flow<List<MonthlyOutcomeCount>>

    @Query(
        "SELECT category, COUNT(*) AS count FROM pantry_items " +
            "WHERE outcome = 'DISCARDED' AND deleted = 0 AND resolvedAt >= :from AND resolvedAt < :to " +
            "GROUP BY category ORDER BY count DESC LIMIT 1"
    )
    fun observeMostDiscarded(from: Long, to: Long): Flow<CategoryCount?>
}
