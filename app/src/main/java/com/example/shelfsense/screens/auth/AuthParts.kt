package com.example.shelfsense.screens.auth

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.shelfsense.R
import com.example.shelfsense.ui.theme.ShelfTheme

// validation shared by the three auth screens, messages carried over from the prototype
object AuthValidation {

    fun email(value: String): String? = when {
        value.isBlank() -> "Enter your email address"
        !Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches() -> "Enter a valid email address"
        else -> null
    }

    fun newPassword(value: String): String? = when {
        value.length < 8 -> "At least 8 characters"
        value.none { it.isLetter() } || value.none { it.isDigit() } -> "Include at least one letter and one number"
        else -> null
    }

    fun name(value: String): String? = when {
        value.isBlank() -> "Enter your name"
        value.trim().length < 2 -> "Name must be at least 2 characters"
        else -> null
    }
}

@Composable
internal fun AuthScaffold(content: @Composable ColumnScope.() -> Unit) {
    val c = ShelfTheme.colors
    Scaffold(containerColor = c.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            content = content
        )
    }
}

@Composable
internal fun BrandBlock() {
    val c = ShelfTheme.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "ShelfSense logo",
            modifier = Modifier.fillMaxWidth(0.5f)
        )
        Spacer(Modifier.height(10.dp))
        Text("Use what you have. Waste less.", style = MaterialTheme.typography.labelLarge, color = c.muted)
    }
}
