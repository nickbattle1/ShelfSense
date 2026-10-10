package com.example.shelfsense.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.shelfsense.data.repository.AuthRepository
import com.example.shelfsense.data.repository.PantryRepository
import com.example.shelfsense.data.repository.SettingsRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

// pushes every pending change to Firestore, then optionally pulls the account's pantry down.
// it's only enqueued with a network constraint, so it never runs offline
class CloudSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val user = AuthRepository(applicationContext).currentUser ?: return Result.success()
        val pantry = PantryRepository(applicationContext)
        val settings = SettingsRepository(applicationContext)
        return try {
            pantry.pushPending(user.uid)
            val prefs = settings.settings.first()
            val name = prefs.displayName
            if (prefs.profilePending && name != null) {
                pantry.pushProfile(user.uid, name, prefs.householdSize)
                settings.markProfileSynced()
            }
            if (inputData.getBoolean(KEY_PULL, false)) {
                pantry.pullFromCloud(user.uid)
                val profile = pantry.fetchProfile(user.uid)
                val cloudName = profile?.first ?: user.displayName
                if (cloudName != null) settings.saveProfile(cloudName, profile?.second, pending = false)
            }
            settings.recordSync(System.currentTimeMillis())
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            // a rules rejection won't fix itself, anything else is worth another go
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED || runAttemptCount >= MAX_ATTEMPTS) {
                Result.failure()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val KEY_PULL = "pull"
        private const val MAX_ATTEMPTS = 5
    }
}
