package com.example.shelfsense.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

enum class PermissionStep { NONE, REQUEST, OPEN_SETTINGS }

// what the notification row or banner should offer next, rechecked whenever the screen resumes
class NotificationAccess(
    val allowed: Boolean,
    val step: PermissionStep,
    private val request: () -> Unit,
    private val openSettingsPage: () -> Unit
) {
    fun resolve() {
        when (step) {
            PermissionStep.REQUEST -> request()
            PermissionStep.OPEN_SETTINGS -> openSettingsPage()
            PermissionStep.NONE -> Unit
        }
    }

    // android never lets an app switch its own notifications off, so this is the way there
    fun openSettings() = openSettingsPage()
}

@Composable
fun rememberNotificationAccess(askedBefore: Boolean, onAsked: () -> Unit): NotificationAccess {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(notificationsAllowed(context)) }
    var granted by remember { mutableStateOf(hasNotificationPermission(context)) }
    var rationale by remember { mutableStateOf(shouldExplainPermission(context)) }

    // coming back from the settings page counts as a resume, so a change there shows up straight away
    LifecycleResumeEffect(Unit) {
        allowed = notificationsAllowed(context)
        granted = hasNotificationPermission(context)
        rationale = shouldExplainPermission(context)
        onPauseOrDispose { }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = notificationsAllowed(context)
        granted = hasNotificationPermission(context)
        rationale = shouldExplainPermission(context)
    }

    // android stops showing the system dialog after two refusals,
    // from then on the app's notification settings page is the only way back
    val step = when {
        allowed -> PermissionStep.NONE
        !granted && (!askedBefore || rationale) -> PermissionStep.REQUEST
        else -> PermissionStep.OPEN_SETTINGS
    }
    return NotificationAccess(
        allowed = allowed,
        step = step,
        request = {
            onAsked()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        openSettingsPage = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            context.startActivity(intent)
        }
    )
}

fun notificationsAllowed(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun shouldExplainPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val activity = context.findActivity() ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
