package com.example.shelfsense.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shelfsense.ui.components.BannerKind
import com.example.shelfsense.ui.components.InfoBanner
import com.example.shelfsense.ui.components.PrimaryButton
import com.example.shelfsense.ui.components.ScreenHeader
import com.example.shelfsense.ui.components.SecondaryButton
import com.example.shelfsense.ui.components.ShelfCard
import com.example.shelfsense.ui.theme.ShelfTheme

@Composable
fun VerifyEmailScreen(
    onVerified: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: VerifyEmailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val c = ShelfTheme.colors

    LaunchedEffect(state.verified) {
        if (state.verified) onVerified()
    }
    LaunchedEffect(state.signedOut) {
        if (state.signedOut) onSignedOut()
    }
    // coming back from the email app counts as a resume, so the link is picked up without a tap
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    AuthScaffold {
        ScreenHeader(title = "Verify your email")
        Text(
            "ShelfSense backs your pantry up to your account, and this email is how you get back in if you forget your password.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.muted
        )
        Spacer(Modifier.height(20.dp))
        ShelfCard {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Filled.MarkEmailUnread,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Check your inbox", style = MaterialTheme.typography.titleMedium, color = c.ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "A verification link was sent to ${state.email}. Open it, then come back here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.muted
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        when {
            state.notYet -> InfoBanner(
                title = "Not verified yet",
                body = "Open the link in the email first. It can take a minute to arrive, so check your junk folder too.",
                kind = BannerKind.WARNING
            )
            state.sent -> InfoBanner(
                title = "A new link is on its way",
                body = "Only the newest link works, so use the one from this email.",
                kind = BannerKind.SUCCESS
            )
            state.error != null -> InfoBanner(title = state.error.orEmpty(), kind = BannerKind.ERROR)
        }
        if (state.notYet || state.sent || state.error != null) Spacer(Modifier.height(16.dp))
        PrimaryButton("I've verified my email", onClick = viewModel::confirm, loading = state.checking)
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Send the email again", onClick = viewModel::resend)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = viewModel::signOut, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Use a different account", style = MaterialTheme.typography.labelLarge, color = c.primary)
        }
        Spacer(Modifier.height(32.dp))
    }
}
