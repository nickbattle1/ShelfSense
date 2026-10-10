package com.example.shelfsense

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelfsense.data.model.ThemeMode
import com.example.shelfsense.data.repository.AuthRepository
import com.example.shelfsense.data.repository.SettingsRepository
import com.example.shelfsense.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// app wide state for MainActivity, so the activity gets the theme and session
// through a ViewModel like every screen does, never from Firebase or DataStore directly
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = AuthRepository(application)

    val themeMode: StateFlow<ThemeMode> = SettingsRepository(application).themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    // read once at launch and kept across rotation: straight into the app,
    // back to the verify screen, or to log in
    val startRoute: String = when {
        auth.hasVerifiedUser -> Routes.MAIN_GRAPH
        auth.currentUser != null -> Routes.VERIFY_EMAIL
        else -> Routes.AUTH_GRAPH
    }

    fun hasAccess(): Boolean = auth.hasVerifiedUser
}
