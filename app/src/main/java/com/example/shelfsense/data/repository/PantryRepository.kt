package com.example.shelfsense.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.WorkInfo
import com.example.shelfsense.data.local.CategoryCount
import com.example.shelfsense.data.local.MonthlyOutcomeCount
import com.example.shelfsense.data.local.OutcomeCount
import com.example.shelfsense.data.local.PantryItem
import com.example.shelfsense.data.local.ShelfSenseDatabase
import com.example.shelfsense.data.model.Outcome
import com.example.shelfsense.data.photos.PhotoStore
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// what the sync worker last did, so Profile can tell "uploading" apart from "gave up"
enum class SyncState { IDLE, RUNNING, FAILED }

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

    // Profile's "Sync now" pulls too, so changes made on another phone come down straight away
    fun requestSync(pull: Boolean = false) {
        WorkScheduler.requestSync(appContext, pull)
    }

    // a failed run only counts while nothing newer is queued. the next request replaces a failed chain,
    // so Sync now or any new change starts afresh
    fun observeSyncState(): Flow<SyncState> =
        WorkScheduler.observeSync(appContext).map { work ->
            when {
                work.any { it.state == WorkInfo.State.RUNNING } -> SyncState.RUNNING
                work.any { it.state == WorkInfo.State.FAILED } &&
                    work.none { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED } -> SyncState.FAILED
                else -> SyncState.IDLE
            }
        }

    suspend fun getItem(id: String): PantryItem? = dao.getById(id)

    suspend fun dueBy(lastDay: LocalDate): List<PantryItem> = dao.getActiveDueBy(lastDay)

    suspend fun countActiveWithBarcode(barcode: String): Int = dao.countActiveWithBarcode(barcode)

    suspend fun save(item: PantryItem) {
        // NonCancellable so leaving the screen mid write can't lose the change
        withContext(NonCancellable) { dao.upsert(item.prepared()) }
        scheduleUpload()
    }

    suspend fun saveAll(items: List<PantryItem>) {
        withContext(NonCancellable) { dao.upsertAll(items.map { it.prepared() }) }
        scheduleUpload()
    }

    // the row is already saved and marked pending, so if scheduling fails the upload just waits
    // for the next launch or Sync now, and the save itself is never reported as failed
    private fun scheduleUpload() {
        runCatching { WorkScheduler.requestSync(appContext) }
    }

    // only used to word the Sync now message, the worker waits for a connection either way
    fun isOnline(): Boolean {
        val connectivity = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
        val photos = PhotoStore(appContext, uid)
        val newer = snapshot.documents.mapNotNull { doc ->
            remoteIds += doc.id
            val remote = FirestoreMapper.fromDocument(doc) ?: return@mapNotNull null
            val local = dao.getById(remote.id)
            if (local == null || remote.updatedAt > local.updatedAt) {
                remote.copy(
                    actionDate = ActionDates.actionDate(remote.printedDate, remote.openedDate, remote.useWithinDays),
                    // photos stay on the phone that took them. after a sign out the row is gone,
                    // but the photo is still in this account's folder under the item's name
                    photoPath = local?.photoPath ?: photos.existingFor(remote.id)
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

    // used when the account is deleted. Firestore doesn't remove documents under a deleted parent,
    // so the pantry goes first in batches, then the profile document
    suspend fun deleteCloudData(uid: String) {
        val pantry = pantryCollection(uid).get(Source.SERVER).await()
        pantry.documents.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
        firestore.collection("users").document(uid).delete().await()
    }

    private fun pantryCollection(uid: String): CollectionReference =
        firestore.collection("users").document(uid).collection("pantry")

    private companion object {
        // Firestore allows 500 writes per batch, this leaves some headroom
        const val BATCH_LIMIT = 400
    }
}
