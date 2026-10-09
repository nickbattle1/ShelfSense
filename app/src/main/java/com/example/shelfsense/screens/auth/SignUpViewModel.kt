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

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val household: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val householdError: String? = null,
    val formError: String? = null,
    val loading: Boolean = false,
    val done: Boolean = false
)

class SignUpViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = AuthRepository(application)
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, formError = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, formError = null) }
    }

    fun onConfirmChange(value: String) {
        _uiState.update { it.copy(confirm = value, confirmError = null, formError = null) }
    }

    fun onHouseholdChange(value: String) {
        _uiState.update { it.copy(household = value, householdError = null, formError = null) }
    }

    fun signUp() {
        val state = _uiState.value
        if (state.loading) return
        val checked = state.copy(
            nameError = AuthValidation.name(state.name),
            emailError = AuthValidation.email(state.email),
            passwordError = AuthValidation.newPassword(state.password),
            confirmError = when {
                state.confirm.isEmpty() -> "Re-enter your password"
                state.confirm != state.password -> "Passwords do not match"
                else -> null
            },
            householdError = if (state.household == null) "Choose your household size" else null
        )
        val household = checked.household
        val hasErrors = listOf(
            checked.nameError, checked.emailError, checked.passwordError, checked.confirmError, checked.householdError
        ).any { it != null }
        if (hasErrors || household == null) {
            _uiState.value = checked
            return
        }
        _uiState.update { it.copy(loading = true, formError = null) }
        viewModelScope.launch {
            try {
                auth.signUp(state.name.trim(), state.email, state.password, household)
                _uiState.update { it.copy(loading = false, done = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, formError = AuthRepository.messageFor(e)) }
            }
        }
    }
}
