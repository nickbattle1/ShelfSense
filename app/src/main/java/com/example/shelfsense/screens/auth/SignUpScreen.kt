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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shelfsense.data.model.Choices
import com.example.shelfsense.ui.components.BannerKind
import com.example.shelfsense.ui.components.DropdownField
import com.example.shelfsense.ui.components.InfoBanner
import com.example.shelfsense.ui.components.PrimaryButton
import com.example.shelfsense.ui.components.ScreenHeader
import com.example.shelfsense.ui.components.TextFieldRow
import com.example.shelfsense.ui.theme.ShelfTheme

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignedUp: () -> Unit,
    viewModel: SignUpViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = ShelfTheme.colors
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.done) {
        if (state.done) onSignedUp()
    }

    AuthScaffold {
        ScreenHeader(title = "Create account", onBack = onBack)
        Text(
            "Set up your ShelfSense account to keep your pantry backed up across devices.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.muted
        )
        Spacer(Modifier.height(22.dp))
        state.formError?.let { message ->
            InfoBanner(title = message, kind = BannerKind.ERROR)
            Spacer(Modifier.height(16.dp))
        }
        TextFieldRow(
            label = "Full name",
            value = state.name,
            onValueChange = viewModel::onNameChange,
            placeholder = "Your name",
            error = state.nameError,
            capitalization = KeyboardCapitalization.Words,
            autofill = ContentType.PersonFullName
        )
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
            placeholder = "At least 8 characters",
            error = state.passwordError,
            helper = "Use at least 8 characters, including a letter and a number",
            isPassword = true,
            autofill = ContentType.NewPassword,
            maxLength = 100
        )
        TextFieldRow(
            label = "Confirm password",
            value = state.confirm,
            onValueChange = viewModel::onConfirmChange,
            placeholder = "Re-enter your password",
            error = state.confirmError,
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.clearFocus() },
            autofill = ContentType.NewPassword,
            maxLength = 100
        )
        DropdownField(
            label = "Household size",
            options = Choices.households,
            selected = state.household,
            onSelect = viewModel::onHouseholdChange,
            optionLabel = { it },
            placeholder = "How many people you shop for",
            error = state.householdError,
            helper = "How many people you shop for"
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            "Create account",
            onClick = {
                focusManager.clearFocus()
                viewModel.signUp()
            },
            loading = state.loading
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Already have an account? Log in", style = MaterialTheme.typography.labelLarge, color = c.primary)
        }
        Spacer(Modifier.height(32.dp))
    }
}
