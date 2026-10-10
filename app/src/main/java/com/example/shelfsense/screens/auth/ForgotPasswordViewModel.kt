package com.example.shelfsense.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.shelfsense.data.repository.AuthRepository
import com.example.shelfsense.navigation.Routes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotUiState(
    val email: String = "",
    val emailError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false,
    val sentTo: String? = null
)

class ForgotPasswordViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val auth = AuthRepository(application)

    // prefilled with whatever was typed on the login screen
    private val _uiState = MutableStateFlow(
        ForgotUiState(email = savedStateHandle.get<String>(Routes.ARG_EMAIL).orEmpty())
    )
    val uiState: StateFlow<ForgotUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, formError = null) }
    }

    fun send() {
        val state = _uiState.value
        if (state.loading) return
        val emailError = AuthValidation.email(state.email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }
        _uiState.update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            try {
                auth.sendPasswordReset(state.email)
                _uiState.update { it.copy(loading = false, sentTo = state.email.trim()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, formError = AuthRepository.messageFor(e)) }
            }
        }
    }
}
