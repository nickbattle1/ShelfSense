package com.example.shelfsense.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.shelfsense.data.repository.PantryRepository
import com.example.shelfsense.data.repository.SettingsRepository
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

// the daily check: finds everything due within the chosen lead time and posts one grouped notification
class ExpiryReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext)
        val prefs = settings.settings.first()
        if (!prefs.remindersEnabled) return Result.success()
        return try {
            val today = LocalDate.now()
            val due = PantryRepository(applicationContext).dueBy(today.plusDays(prefs.leadDays.toLong()))
            ReminderNotifier.show(applicationContext, due, today)
            settings.recordReminderCheck(System.currentTimeMillis(), due.size)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
