package com.example.shelfsense.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// snackbars live in the root scaffold so an undo prompt outlives the screen that raised it
class AppMessenger(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    fun show(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = actionLabel != null,
                duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

val LocalMessenger = staticCompositionLocalOf<AppMessenger> {
    error("AppMessenger has not been provided")
}
