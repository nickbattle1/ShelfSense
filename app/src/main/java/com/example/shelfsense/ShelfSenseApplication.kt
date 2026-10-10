package com.example.shelfsense

import android.app.Application
import com.example.shelfsense.data.repository.AuthRepository
import com.example.shelfsense.data.repository.PantryRepository
import com.example.shelfsense.data.repository.SettingsRepository
import com.example.shelfsense.worker.ReminderNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShelfSenseApplication : Application() {

    // outlives any single screen, used for undo and other writes that must finish after navigating away
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.createChannel(this)
        applicationScope.launch {
            SettingsRepository(this@ShelfSenseApplication).restoreReminderSchedule()
            // uploads anything saved offline, then pulls changes made on another phone
            if (AuthRepository(this@ShelfSenseApplication).currentUser != null) {
                PantryRepository(this@ShelfSenseApplication).requestSync(pull = true)
            }
        }
    }
}
