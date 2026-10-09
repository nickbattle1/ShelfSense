package com.example.shelfsense.screens.addfood

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.shelfsense.domain.Barcodes
import com.example.shelfsense.ui.components.PrimaryButton
import com.example.shelfsense.ui.components.ScreenHeader
import com.example.shelfsense.ui.components.TextFieldRow
import com.example.shelfsense.ui.theme.ShelfTheme

@Composable
fun EnterBarcodeScreen(onBack: () -> Unit, onLookUp: (String) -> Unit) {
    val c = ShelfTheme.colors
    val focusManager = LocalFocusManager.current
    var code by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val submit = {
        val problem = Barcodes.problemWith(code)
        error = problem
        if (problem == null) {
            focusManager.clearFocus()
            onLookUp(code)
        }
    }

    Scaffold(containerColor = c.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            ScreenHeader(title = "Enter barcode", onBack = onBack)
            Text(
                "Type the number printed underneath the barcode and ShelfSense will fill in the product details.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.muted
            )
            Spacer(Modifier.height(22.dp))
            TextFieldRow(
                label = "Barcode",
                value = code,
                onValueChange = {
                    code = Barcodes.clean(it)
                    error = null
                },
                placeholder = "9300675024235",
                error = error,
                helper = "Usually 13 digits, printed under the bars",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Search,
                onImeAction = submit,
                maxLength = 14
            )
            Spacer(Modifier.height(8.dp))
            PrimaryButton("Look up product", onClick = submit, enabled = code.length >= 8)
            Spacer(Modifier.height(14.dp))
            Text(
                "Product details come from Open Food Facts, which is contributed by the public, so some items are missing.",
                style = MaterialTheme.typography.labelSmall,
                color = c.muted
            )
        }
    }
}
