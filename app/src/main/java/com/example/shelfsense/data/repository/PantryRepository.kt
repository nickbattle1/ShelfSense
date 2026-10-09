package com.example.shelfsense.data.repository

import android.content.Context
import com.example.shelfsense.data.local.CategoryCount
import com.example.shelfsense.data.local.MonthlyOutcomeCount
import com.example.shelfsense.data.local.OutcomeCount
import com.example.shelfsense.data.local.PantryItem
import com.example.shelfsense.data.local.ShelfSenseDatabase
import com.example.shelfsense.data.model.Outcome
import com.example.shelfsense.data.remote.FirestoreMapper
import com.example.shelfsense.domain.ActionDates
import com.example.shelfsense.worker.WorkScheduler
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import java.time.LocalDate
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// offline first: every change lands in Room straight away and is queued for Firestore,
// which CloudSyncWorker pushes whenever the device has a connection
class PantryRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = ShelfSenseDatabase.getDatabase(appContext).pantryItemDao()
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    val activeItems: Flow<List<PantryItem>> = dao.observeActive()

    fun observeItem(id: String): Flow<PantryItem?> = dao.observeById(id)

    fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    fun observeOutcomeCounts(from: Long, to: Long): Flow<List<OutcomeCount>> =
        dao.observeOutcomeCounts(from, to)

    fun observeMonthlyOutcomes(from: Long): Flow<List<MonthlyOutcomeCount>> =
        dao.observeMonthlyOutcomes(from)

    fun observeMostDiscarded(from: Long, to: Long): Flow<CategoryCount?> =
        dao.observeMostDiscarded(from, to)

    suspend fun getItem(id: String): PantryItem? = dao.getById(id)

    suspend fun dueBy(lastDay: LocalDate): List<PantryItem> = dao.getActiveDueBy(lastDay)

    suspend fun countActiveWithBarcode(barcode: String): Int = dao.countActiveWithBarcode(barcode)

    suspend fun save(item: PantryItem) {
        // NonCancellable so leaving the screen mid write can't lose the change
        withContext(NonCancellable) { dao.upsert(item.prepared()) }
        WorkScheduler.requestSync(appContext)
    }

    suspend fun saveAll(items: List<PantryItem>) {
        withContext(NonCancellable) { dao.upsertAll(items.map { it.prepared() }) }
        WorkScheduler.requestSync(appContext)
    }

    suspend fun recordOutcome(item: PantryItem, outcome: Outcome) {
        save(item.copy(outcome = outcome, resolvedAt = System.currentTimeMillis()))
    }

    suspend fun markOpened(item: PantryItem, openedDate: LocalDate, useWithinDays: Int?) {
        save(item.copy(openedDate = openedDate, useWithinDays = useWithinDays))
    }

    suspend fun delete(item: PantryItem) {
        save(item.copy(deleted = true))
    }

    // undo writes the earlier copy back, which works even if the delete already reached Firestore
    suspend fun restore(snapshot: PantryItem) {
        save(snapshot.copy(deleted = false))
    }

    suspend fun clearLocal() {
        dao.clearAll()
    }

    private fun PantryItem.prepared(): PantryItem = copy(
        actionDate = ActionDates.actionDate(printedDate, openedDate, useWithinDays),
        updatedAt = System.currentTimeMillis(),
        syncPending = true
    )

    // called by CloudSyncWorker. batched writes keep it to one round trip for most pantries
    suspend fun pushPending(uid: String): Int {
        val pending = dao.getPendingSync()
        val collection = pantryCollection(uid)
        pending.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { item ->
                val doc = collection.document(item.id)
                if (item.deleted) batch.delete(doc) else batch.set(doc, FirestoreMapper.toDocument(item))
            }
            batch.commit().await()
            // the flag only clears if the row wasn't edited again while the upload was in flight
            chunk.forEach { item ->
                if (item.deleted) {
                    dao.purgeDeleted(item.id, item.updatedAt)
                } else {
                    dao.markSynced(item.id, item.updatedAt)
                }
            }
        }
        return pending.size
    }

    // runs on sign in and every app start rather than as a live listener, which keeps reads
    // inside the free tier. when both copies changed, the one edited most recently wins
    suspend fun pullFromCloud(uid: String): Int {
        val snapshot = pantryCollection(uid).get(Source.SERVER).await()
        val remoteIds = HashSet<String>()
        val newer = snapshot.documents.mapNotNull { doc ->
            remoteIds += doc.id
            val remote = FirestoreMapper.fromDocument(doc) ?: return@mapNotNull null
            val local = dao.getById(remote.id)
            if (local == null || remote.updatedAt > local.updatedAt) {
                remote.copy(
                    actionDate = ActionDates.actionDate(remote.printedDate, remote.openedDate, remote.useWithinDays),
                    // photos stay on the phone that took them, so the local one is kept
                    photoPath = local?.photoPath
                )
            } else {
                null
            }
        }
        if (newer.isNotEmpty()) dao.upsertAll(newer)
        // anything already synced that's missing from the cloud was deleted on another device
        val removed = dao.getSyncedIds().filterNot { it in remoteIds }
        removed.chunked(BATCH_LIMIT).forEach { dao.deleteSynced(it) }
        return newer.size
    }

    suspend fun pushProfile(uid: String, name: String, household: String?) {
        val data = hashMapOf<String, Any?>(
            "displayName" to name,
            "householdSize" to household,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(uid).set(data, SetOptions.merge()).await()
    }

    // returns the stored name and household size, or null when no profile document exists yet
    suspend fun fetchProfile(uid: String): Pair<String?, String?>? {
        val doc = firestore.collection("users").document(uid).get(Source.SERVER).await()
        if (!doc.exists()) return null
        return doc.getString("displayName") to doc.getString("householdSize")
    }

    private fun pantryCollection(uid: String): CollectionReference =
        firestore.collection("users").document(uid).collection("pantry")

    private companion object {
        // Firestore allows 500 writes per batch, this leaves some headroom
        const val BATCH_LIMIT = 400
    }
}
