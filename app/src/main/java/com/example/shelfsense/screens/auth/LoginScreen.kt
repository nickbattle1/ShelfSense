package com.example.shelfsense.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shelfsense.ui.components.BannerKind
import com.example.shelfsense.ui.components.InfoBanner
import com.example.shelfsense.ui.components.PrimaryButton
import com.example.shelfsense.ui.components.TextFieldRow
import com.example.shelfsense.ui.theme.ShelfTheme

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: (String) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = ShelfTheme.colors
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        viewModel.logIn()
    }

    LaunchedEffect(state.signedIn) {
        if (state.signedIn) onLoggedIn()
    }

    AuthScaffold {
        Spacer(Modifier.height(40.dp))
        BrandBlock()
        Spacer(Modifier.height(36.dp))
        Text(
            "Log in",
            style = MaterialTheme.typography.titleLarge,
            color = c.ink,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(18.dp))
        state.formError?.let { message ->
            InfoBanner(title = message, kind = BannerKind.ERROR)
            Spacer(Modifier.height(16.dp))
        }
        TextFieldRow(
            label = "Email",
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            placeholder = "you@example.com",
            error = state.emailError,
            keyboardType = KeyboardType.Email,
            autofill = ContentType.EmailAddress,
            maxLength = 100
        )
        TextFieldRow(
            label = "Password",
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = "Enter your password",
            error = state.passwordError,
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = submit,
            autofill = ContentType.Password,
            maxLength = 100
        )
        TextButton(
            onClick = { onForgotPassword(state.email.trim()) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Forgot password?", style = MaterialTheme.typography.labelMedium, color = c.primary)
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Log in", onClick = submit, loading = state.loading)
        Spacer(Modifier.height(20.dp))
        Text(
            "New here?",
            style = MaterialTheme.typography.bodyMedium,
            color = c.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = onCreateAccount, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Create an account", style = MaterialTheme.typography.labelLarge, color = c.primary)
        }
        Spacer(Modifier.height(32.dp))
    }
}
