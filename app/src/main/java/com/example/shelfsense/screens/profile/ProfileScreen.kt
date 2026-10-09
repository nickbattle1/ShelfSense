package com.example.shelfsense.screens.profile

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shelfsense.data.model.Choices
import com.example.shelfsense.data.model.ThemeMode
import com.example.shelfsense.domain.Dates
import com.example.shelfsense.screens.auth.AuthValidation
import com.example.shelfsense.ui.components.*
import com.example.shelfsense.ui.theme.ShelfTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onSignedOut: () -> Unit, viewModel: ProfileViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = ShelfTheme.colors
    val context = LocalContext.current
    val messenger = LocalMessenger.current
    val scope = rememberCoroutineScope()
    val access = rememberNotificationAccess(state.notificationAsked, viewModel::markNotificationAsked)
    // sample data is a testing aid, so release builds never show it
    val debuggable = remember { (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }
    val editSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // picks up a verification done in the email app as soon as the person comes back
    LifecycleResumeEffect(Unit) {
        viewModel.refreshVerification()
        onPauseOrDispose { }
    }

    val toggleReminders: (Boolean) -> Unit = { on ->
        viewModel.setReminders(on)
        if (on && access.step == PermissionStep.REQUEST) access.resolve()
    }

    Scaffold(containerColor = c.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            ScreenHeader(title = "Profile")
            AccountCard(
                state = state,
                onEdit = { editing = true },
                onResend = { viewModel.resendVerification { message -> messenger.show(message) } }
            )

            SectionLabel("Reminders")
            ShelfCard {
                // the whole row toggles, so TalkBack reads the label together with the switch state
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = state.remindersEnabled, role = Role.Switch, onValueChange = toggleReminders),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Expiry reminders", style = MaterialTheme.typography.titleMedium, color = c.ink)
                        Text("Daily background check", style = MaterialTheme.typography.bodyMedium, color = c.muted)
                    }
                    Switch(
                        checked = state.remindersEnabled,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = c.onPrimary,
                            checkedTrackColor = c.primary,
                            uncheckedThumbColor = c.muted,
                            uncheckedTrackColor = c.chip,
                            uncheckedBorderColor = c.line
                        )
                    )
                }
                CardDivider()
                DropdownField(
                    label = "Remind me",
                    options = Choices.leadDays,
                    selected = state.leadDays,
                    onSelect = viewModel::setLeadDays,
                    optionLabel = { Choices.leadLabel(it) },
                    enabled = state.remindersEnabled,
                    helper = "How far ahead of an item's action date you'll hear about it"
                )
                DetailRow("Last check", state.lastCheck?.let { Dates.relative(it) } ?: "Not run yet")
                if (state.lastCheck != null) {
                    Text(
                        if (state.lastCheckCount == 1) "1 item was due" else "${state.lastCheckCount} items were due",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.muted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                SecondaryButton(
                    "Check now",
                    enabled = state.remindersEnabled,
                    onClick = {
                        viewModel.checkNow()
                        messenger.show(
                            if (access.allowed) {
                                "Checking your pantry now"
                            } else {
                                "Checking now, but notifications are off so nothing will pop up"
                            }
                        )
                    }
                )
            }

            SectionLabel("Permissions and sync")
            ShelfCard {
                StatusRow(
                    icon = if (access.allowed) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                    title = "Notifications",
                    status = if (access.allowed) "Notifications allowed" else "Notifications are off, so reminders can't appear",
                    actionLabel = when (access.step) {
                        PermissionStep.NONE -> null
                        PermissionStep.REQUEST -> "Allow"
                        PermissionStep.OPEN_SETTINGS -> "Open settings"
                    },
                    onAction = access::resolve,
                    warning = !access.allowed
                )
                CardDivider()
                StatusRow(
                    icon = when {
                        state.syncing -> Icons.Filled.CloudSync
                        state.pendingCount > 0 -> Icons.Filled.CloudUpload
                        else -> Icons.Filled.CloudDone
                    },
                    title = syncTitle(state),
                    status = state.lastSync
                        ?.let { "Last sync ${Dates.relative(it).replaceFirstChar { ch -> ch.lowercase() }}" }
                        ?: "Syncs automatically whenever you're online",
                    actionLabel = if (state.syncing) null else "Sync now",
                    onAction = {
                        viewModel.syncNow()
                        messenger.show("Syncing with your account")
                    }
                )
            }

            SectionLabel("Appearance")
            ShelfCard {
                DropdownField(
                    label = "Theme",
                    options = ThemeMode.entries,
                    selected = state.themeMode,
                    onSelect = viewModel::setTheme,
                    optionLabel = { it.label }
                )
            }

            if (debuggable) {
                SectionLabel("Testing")
                ShelfCard {
                    Text(
                        "Load a sample pantry with six months of history to try the reports and reminders. Only shown in debug builds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.muted
                    )
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton(
                        "Load sample data",
                        onClick = { viewModel.loadSampleData { count -> messenger.show("Added $count sample items") } }
                    )
                }
            }

            SectionLabel("About")
            ShelfCard {
                Text("ShelfSense 1.0", style = MaterialTheme.typography.labelLarge, color = c.ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Product details come from Open Food Facts, available under the Open Database License.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.muted
                )
            }
            Spacer(Modifier.height(24.dp))
            SecondaryButton(
                "Sign out",
                onClick = { confirmSignOut = true },
                enabled = !signingOut,
                contentColor = c.urgent
            )
            Spacer(Modifier.height(40.dp))
        }
    }

    if (editing) {
        ModalBottomSheet(
            onDismissRequest = { editing = false },
            sheetState = editSheet,
            containerColor = c.surface
        ) {
            EditProfileSheet(
                initialName = state.name,
                initialHousehold = state.household,
                onSave = { name, household ->
                    viewModel.updateProfile(name, household)
                    scope.launch { editSheet.hide() }.invokeOnCompletion { editing = false }
                    messenger.show("Profile updated")
                },
                onCancel = {
                    scope.launch { editSheet.hide() }.invokeOnCompletion { editing = false }
                }
            )
        }
    }

    if (confirmSignOut) {
        val pending = state.pendingCount
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = {
                Text(
                    if (pending > 0) {
                        "${if (pending == 1) "1 change hasn't" else "$pending changes haven't"} uploaded yet " +
                            "and will be lost if you sign out now. Connect to the internet and tap Sync now first."
                    } else {
                        "Your pantry is backed up to your account and comes back when you log in again."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    signingOut = true
                    viewModel.signOut(onSignedOut)
                }) { Text("Sign out", color = c.urgent) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") }
            },
            containerColor = c.surface
        )
    }
}

private fun syncTitle(state: ProfileUiState): String = when {
    state.syncing -> "Syncing…"
    state.pendingCount == 1 -> "1 change waiting to upload"
    state.pendingCount > 1 -> "${state.pendingCount} changes waiting to upload"
    state.lastSync != null -> "All changes synced"
    else -> "Not synced yet"
}

@Composable
private fun AccountCard(state: ProfileUiState, onEdit: () -> Unit, onResend: () -> Unit) {
    val c = ShelfTheme.colors
    val initials = state.name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
    ShelfCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(c.tint, CircleShape), contentAlignment = Alignment.Center) {
                Text(initials, style = MaterialTheme.typography.titleMedium, color = c.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(state.name, style = MaterialTheme.typography.titleMedium, color = c.ink)
                if (state.email.isNotBlank()) {
                    Text(state.email, style = MaterialTheme.typography.bodyMedium, color = c.muted)
                }
                state.household?.let {
                    Text("Household: $it", style = MaterialTheme.typography.labelSmall, color = c.muted)
                }
            }
            TextButton(onClick = onEdit) {
                Text("Edit", style = MaterialTheme.typography.labelLarge, color = c.primary)
            }
        }
        // a nudge rather than a gate, so testers with throwaway emails can still use the app
        if (!state.emailVerified) {
            CardDivider()
            StatusRow(
                icon = Icons.Filled.MarkEmailUnread,
                title = "Email not verified",
                status = "Open the verification link sent to your inbox to confirm it's yours",
                actionLabel = "Resend",
                onAction = onResend,
                warning = true
            )
        }
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    title: String,
    status: String,
    actionLabel: String?,
    onAction: () -> Unit,
    warning: Boolean = false
) {
    val c = ShelfTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = if (warning) c.warn else c.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = c.ink)
            Text(status, style = MaterialTheme.typography.labelSmall, color = c.muted)
        }
        if (actionLabel != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = c.primary)
            }
        }
    }
}

@Composable
private fun EditProfileSheet(
    initialName: String,
    initialHousehold: String?,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    val c = ShelfTheme.colors
    var name by rememberSaveable { mutableStateOf(initialName) }
    var household by rememberSaveable { mutableStateOf(initialHousehold) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    val nameError = AuthValidation.name(name)
    val householdError = if (household == null) "Choose your household size" else null

    Column(
        Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            "Edit profile",
            style = MaterialTheme.typography.titleLarge,
            color = c.ink,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(16.dp))
        TextFieldRow(
            label = "Full name",
            value = name,
            onValueChange = { name = it },
            placeholder = "Your name",
            error = if (showErrors) nameError else null,
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        )
        DropdownField(
            label = "Household size",
            options = Choices.households,
            selected = household,
            onSelect = { household = it },
            optionLabel = { it },
            placeholder = "How many people you shop for",
            error = if (showErrors) householdError else null
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Save", onClick = {
            showErrors = true
            val chosen = household
            if (nameError == null && chosen != null) onSave(name.trim(), chosen)
        })
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Cancel", onClick = onCancel)
    }
}
