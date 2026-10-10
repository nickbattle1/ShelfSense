package com.example.shelfsense.screens.profile

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    var changingPassword by rememberSaveable { mutableStateOf(false) }
    var deletingAccount by rememberSaveable { mutableStateOf(false) }
    val passwordSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val deleteSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val passwordStatus by viewModel.passwordStatus.collectAsStateWithLifecycle()
    val deleteStatus by viewModel.deleteStatus.collectAsStateWithLifecycle()

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
            AccountCard(state = state, onEdit = { editing = true })

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

            SectionLabel("Notifications and backup")
            ShelfCard {
                // android only lets an app ask for notifications, so switching them off happens on the
                // system page. the reminders switch above is the in-app on and off
                StatusRow(
                    icon = if (access.allowed) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                    title = "Notifications",
                    status = if (access.allowed) "Notifications allowed" else "Notifications are off, so reminders can't appear",
                    actionLabel = when (access.step) {
                        PermissionStep.NONE -> "Manage"
                        PermissionStep.REQUEST -> "Allow"
                        PermissionStep.OPEN_SETTINGS -> "Open settings"
                    },
                    onAction = { if (access.allowed) access.openSettings() else access.resolve() },
                    warning = !access.allowed
                )
                CardDivider()
                StatusRow(
                    icon = when {
                        state.syncing -> Icons.Filled.CloudSync
                        state.pendingCount > 0 -> Icons.Filled.CloudUpload
                        else -> Icons.Filled.CloudDone
                    },
                    title = backupTitle(state),
                    status = backupStatus(state),
                    actionLabel = if (state.syncing) null else "Sync now",
                    onAction = {
                        viewModel.syncNow()
                        messenger.show("Backing up, and fetching any changes from your other devices")
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
                SectionLabel("Sample data")
                ShelfCard {
                    Text(
                        "Adds 13 pantry items and six months of past outcomes, so Insights has a trend to show. " +
                            "Tapping it again resets the samples rather than doubling them.",
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

            SectionLabel("Account")
            ShelfCard {
                ActionRow(
                    icon = Icons.Filled.Lock,
                    title = "Change password",
                    subtitle = "Update the password you log in with",
                    onClick = { changingPassword = true }
                )
                CardDivider()
                ActionRow(
                    icon = Icons.Filled.DeleteForever,
                    title = "Delete account",
                    subtitle = "Permanently removes your account, pantry and backup",
                    onClick = { deletingAccount = true },
                    destructive = true
                )
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

    if (changingPassword) {
        val close: () -> Unit = {
            scope.launch { passwordSheet.hide() }.invokeOnCompletion {
                changingPassword = false
                viewModel.clearSheetStatus()
            }
        }
        ModalBottomSheet(
            onDismissRequest = {
                changingPassword = false
                viewModel.clearSheetStatus()
            },
            sheetState = passwordSheet,
            containerColor = c.surface
        ) {
            ChangePasswordSheet(
                status = passwordStatus,
                onSave = { current, newPassword ->
                    viewModel.changePassword(current, newPassword) {
                        close()
                        messenger.show("Password changed")
                    }
                },
                onCancel = close
            )
        }
    }

    if (deletingAccount) {
        ModalBottomSheet(
            onDismissRequest = {
                deletingAccount = false
                viewModel.clearSheetStatus()
            },
            sheetState = deleteSheet,
            containerColor = c.surface
        ) {
            DeleteAccountSheet(
                status = deleteStatus,
                onDelete = { password ->
                    viewModel.deleteAccount(password) {
                        deletingAccount = false
                        messenger.show("Your account has been deleted")
                        onSignedOut()
                    }
                },
                onCancel = {
                    scope.launch { deleteSheet.hide() }.invokeOnCompletion {
                        deletingAccount = false
                        viewModel.clearSheetStatus()
                    }
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

// sync in plain words: the phone keeps the working copy and Firestore holds the backup
private fun backupTitle(state: ProfileUiState): String = when {
    state.syncing -> "Backing up…"
    state.pendingCount == 1 -> "1 change not backed up yet"
    state.pendingCount > 1 -> "${state.pendingCount} changes not backed up yet"
    state.lastSync != null -> "Pantry backed up"
    else -> "Not backed up yet"
}

private fun backupStatus(state: ProfileUiState): String = when {
    state.pendingCount > 0 -> "Uploads by itself once you're online"
    state.lastSync != null -> "Last synced ${Dates.relative(state.lastSync).replaceFirstChar { it.lowercase() }}"
    else -> "Backs up to your account whenever you're online"
}

@Composable
private fun AccountCard(state: ProfileUiState, onEdit: () -> Unit) {
    val c = ShelfTheme.colors
    val initials = state.name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
    ShelfCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(c.tint, CircleShape), contentAlignment = Alignment.Center) {
                Text(initials, style = MaterialTheme.typography.titleMedium, color = c.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.email.isNotBlank()) {
                    Text(
                        state.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                state.household?.let { HouseholdTag(it) }
            }
            TextButton(onClick = onEdit) {
                Text("Edit", style = MaterialTheme.typography.labelLarge, color = c.primary)
            }
        }
    }
}

// a small tag set apart from the name and email, so the three don't read as one block
@Composable
private fun HouseholdTag(household: String) {
    val c = ShelfTheme.colors
    Row(
        Modifier
            .padding(top = 10.dp)
            .background(c.chip, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Household size, $household" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Groups, contentDescription = null, tint = c.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(household, style = MaterialTheme.typography.labelMedium, color = c.ink)
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val c = ShelfTheme.colors
    val tint = if (destructive) c.urgent else c.primary
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = if (destructive) c.urgent else c.ink)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = c.muted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.muted)
    }
}

// passwords live in plain remember rather than rememberSaveable, so they're never written to saved state
@Composable
private fun ChangePasswordSheet(status: SheetStatus, onSave: (String, String) -> Unit, onCancel: () -> Unit) {
    val c = ShelfTheme.colors
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    val currentError = if (current.isEmpty()) "Enter your current password" else null
    val newError = AuthValidation.newPassword(newPassword)
        ?: if (newPassword == current) "Choose a different password from your current one" else null
    val confirmError = when {
        confirm.isEmpty() -> "Re-enter your new password"
        confirm != newPassword -> "Passwords do not match"
        else -> null
    }

    Column(
        Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            "Change password",
            style = MaterialTheme.typography.titleLarge,
            color = c.ink,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "For your security, enter your current password before choosing a new one.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.muted
        )
        Spacer(Modifier.height(16.dp))
        status.error?.let { message ->
            InfoBanner(title = message, kind = BannerKind.ERROR)
            Spacer(Modifier.height(16.dp))
        }
        TextFieldRow(
            label = "Current password",
            value = current,
            onValueChange = { current = it },
            placeholder = "Enter your current password",
            error = if (showErrors) currentError else null,
            isPassword = true,
            autofill = ContentType.Password,
            maxLength = 100
        )
        TextFieldRow(
            label = "New password",
            value = newPassword,
            onValueChange = { newPassword = it },
            placeholder = "At least 8 characters",
            error = if (showErrors) newError else null,
            helper = "Use at least 8 characters, including a letter and a number",
            isPassword = true,
            autofill = ContentType.NewPassword,
            maxLength = 100
        )
        TextFieldRow(
            label = "Confirm new password",
            value = confirm,
            onValueChange = { confirm = it },
            placeholder = "Re-enter your new password",
            error = if (showErrors) confirmError else null,
            isPassword = true,
            imeAction = ImeAction.Done,
            autofill = ContentType.NewPassword,
            maxLength = 100
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Change password", loading = status.busy, onClick = {
            showErrors = true
            if (currentError == null && newError == null && confirmError == null) onSave(current, newPassword)
        })
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Cancel", onClick = onCancel)
    }
}

@Composable
private fun DeleteAccountSheet(status: SheetStatus, onDelete: (String) -> Unit, onCancel: () -> Unit) {
    val c = ShelfTheme.colors
    var password by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    val passwordError = if (password.isEmpty()) "Enter your password to confirm" else null

    Column(
        Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            "Delete your account?",
            style = MaterialTheme.typography.titleLarge,
            color = c.ink,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "This permanently deletes your account, every item in your pantry, your history and the backup in the cloud. It can't be undone.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.muted
        )
        Spacer(Modifier.height(16.dp))
        status.error?.let { message ->
            InfoBanner(title = message, kind = BannerKind.ERROR)
            Spacer(Modifier.height(16.dp))
        }
        TextFieldRow(
            label = "Password",
            value = password,
            onValueChange = { password = it },
            placeholder = "Enter your password",
            error = if (showErrors) passwordError else null,
            isPassword = true,
            imeAction = ImeAction.Done,
            autofill = ContentType.Password,
            maxLength = 100
        )
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Delete account", loading = status.busy, destructive = true, onClick = {
            showErrors = true
            if (passwordError == null) onDelete(password)
        })
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Keep my account", onClick = onCancel)
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
