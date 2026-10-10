package com.example.shelfsense

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelfsense.data.model.ThemeMode
import com.example.shelfsense.data.repository.AuthRepository
import com.example.shelfsense.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// app wide state for MainActivity, so the activity gets the theme and session
// through a ViewModel like every screen does, never from Firebase or DataStore directly
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = AuthRepository(application)

    val themeMode: StateFlow<ThemeMode> = SettingsRepository(application).themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    // read once at launch to pick the start destination, and kept across rotation
    val startSignedIn: Boolean = auth.currentUser != null

    fun isSignedIn(): Boolean = auth.currentUser != null
}
