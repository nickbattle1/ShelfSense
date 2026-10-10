package com.example.shelfsense.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelfsense.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerifyUiState(
    val email: String = "",
    val checking: Boolean = false,
    val notYet: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
    val verified: Boolean = false,
    val signedOut: Boolean = false
)

// the account is created and signed in, but the app stays closed until the email is confirmed.
// the email is the only way back in after a forgotten password, so it has to be real
class VerifyEmailViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = AuthRepository(application)
    private val _uiState = MutableStateFlow(VerifyUiState(email = auth.currentUser?.email.orEmpty()))
    val uiState: StateFlow<VerifyUiState> = _uiState.asStateFlow()

    // a quiet check whenever the screen comes back into view, e.g. after tapping the link in the email app
    fun refresh() {
        viewModelScope.launch {
            if (auth.refreshVerified()) _uiState.update { it.copy(verified = true) }
        }
    }

    fun confirm() {
        if (_uiState.value.checking) return
        _uiState.update { it.copy(checking = true, notYet = false, sent = false, error = null) }
        viewModelScope.launch {
            val verified = auth.refreshVerified()
            _uiState.update { it.copy(checking = false, verified = verified, notYet = !verified) }
        }
    }

    fun resend() {
        _uiState.update { it.copy(notYet = false, sent = false, error = null) }
        viewModelScope.launch {
            try {
                auth.sendVerification()
                _uiState.update { it.copy(sent = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = AuthRepository.messageFor(e)) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            _uiState.update { it.copy(signedOut = true) }
        }
    }
}
