package com.example.shelfsense.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

// every piece of background work is enqueued from here, always as unique work
// so repeated calls never stack up duplicate jobs
object WorkScheduler {

    const val SYNC_WORK = "cloud_sync"
    const val REMINDER_WORK = "expiry_reminders"
    const val REMINDER_NOW_WORK = "expiry_check_now"

    fun requestSync(context: Context, pull: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(CloudSyncWorker.KEY_PULL to pull))
            .build()
        // appended, so a change made mid sync still gets its own run once the current one finishes
        WorkManager.getInstance(context)
            .enqueueUniqueWork(SYNC_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun cancelSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK)
    }

    fun observeSync(context: Context): Flow<List<WorkInfo>> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(SYNC_WORK)

    // a daily check with a one hour flex window. the first run is lined up for about 8am,
    // and KEEP means reopening the app never resets the schedule
    fun scheduleReminders(context: Context) {
        val request = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(1, TimeUnit.DAYS, 1, TimeUnit.HOURS)
            .setInitialDelay(millisUntilMorning(), TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(REMINDER_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK)
    }

    // the Profile screen's "Check now" runs the same worker once, straight away
    fun checkRemindersNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<ExpiryReminderWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(REMINDER_NOW_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    private fun millisUntilMorning(now: LocalDateTime = LocalDateTime.now()): Long {
        var next = now.toLocalDate().atTime(8, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }
}
