package com.example.shelfsense

import android.app.Application
import com.example.shelfsense.data.repository.SettingsRepository
import com.example.shelfsense.worker.ReminderNotifier
import com.example.shelfsense.worker.WorkScheduler
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShelfSenseApplication : Application() {

    // outlives any single screen, used for undo and other writes that must finish after navigating away
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.createChannel(this)
        applicationScope.launch {
            if (SettingsRepository(this@ShelfSenseApplication).settings.first().remindersEnabled) {
                WorkScheduler.scheduleReminders(this@ShelfSenseApplication)
            }
            // anything saved offline last session gets another chance to upload
            if (FirebaseAuth.getInstance().currentUser != null) {
                WorkScheduler.requestSync(this@ShelfSenseApplication)
            }
        }
    }
}
