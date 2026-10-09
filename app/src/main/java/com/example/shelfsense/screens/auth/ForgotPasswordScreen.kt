package com.example.shelfsense.screens.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shelfsense.ui.components.BannerKind
import com.example.shelfsense.ui.components.InfoBanner
import com.example.shelfsense.ui.components.PrimaryButton
import com.example.shelfsense.ui.components.ScreenHeader
import com.example.shelfsense.ui.components.SecondaryButton
import com.example.shelfsense.ui.components.TextFieldRow
import com.example.shelfsense.ui.theme.ShelfTheme

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, viewModel: ForgotPasswordViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = ShelfTheme.colors
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        viewModel.send()
    }

    AuthScaffold {
        ScreenHeader(title = "Reset password", onBack = onBack)
        Text(
            "Enter the email address on your account and ShelfSense will send a reset link.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.muted
        )
        Spacer(Modifier.height(22.dp))
        val sentTo = state.sentTo
        if (sentTo != null) {
            InfoBanner(
                title = "Email sent",
                body = "If an account exists for $sentTo, a reset link is on its way. " +
                    "Check your inbox and spam folder for the reset link.",
                kind = BannerKind.SUCCESS
            )
            Spacer(Modifier.height(20.dp))
            SecondaryButton("Back to log in", onClick = onBack)
        } else {
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
                imeAction = ImeAction.Done,
                onImeAction = submit,
                autofill = ContentType.EmailAddress,
                maxLength = 100
            )
            Spacer(Modifier.height(8.dp))
            PrimaryButton("Send reset link", onClick = submit, loading = state.loading)
        }
    }
}
