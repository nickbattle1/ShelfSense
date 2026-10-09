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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false,
    val signedIn: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = AuthRepository(application)
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, formError = null) }
    }

    fun logIn() {
        val state = _uiState.value
        if (state.loading) return
        val emailError = AuthValidation.email(state.email)
        val passwordError = if (state.password.isEmpty()) "Password is required" else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _uiState.update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            try {
                auth.signIn(state.email, state.password)
                _uiState.update { it.copy(loading = false, signedIn = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, formError = AuthRepository.messageFor(e)) }
            }
        }
    }
}
