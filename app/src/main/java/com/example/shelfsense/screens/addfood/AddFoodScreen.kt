package com.example.shelfsense.screens.addfood

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.shelfsense.ui.components.BannerKind
import com.example.shelfsense.ui.components.InfoBanner
import com.example.shelfsense.ui.components.ScreenHeader
import com.example.shelfsense.ui.theme.ShelfTheme

@Composable
fun AddFoodScreen(
    onBack: () -> Unit,
    onScan: () -> Unit,
    onEnterBarcode: () -> Unit,
    onManual: () -> Unit
) {
    val c = ShelfTheme.colors
    Scaffold(containerColor = c.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            ScreenHeader(title = "Add food", onBack = onBack)
            Text(
                "Look the product up to fill in the details, or enter it yourself.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.muted
            )
            Spacer(Modifier.height(20.dp))
            AddOption(
                icon = Icons.Filled.QrCodeScanner,
                title = "Scan barcode",
                body = "Point the camera at the package and ShelfSense reads the number",
                onClick = onScan
            )
            Spacer(Modifier.height(12.dp))
            AddOption(
                icon = Icons.Filled.Dialpad,
                title = "Enter barcode",
                body = "Type the number yourself if the camera can't read it",
                onClick = onEnterBarcode
            )
            Spacer(Modifier.height(12.dp))
            AddOption(
                icon = Icons.Filled.EditNote,
                title = "Add manually",
                body = "Fresh produce, leftovers and anything without a barcode",
                onClick = onManual
            )
            Spacer(Modifier.height(20.dp))
            InfoBanner(
                title = "No camera permission needed",
                body = "Scanning runs through Google's code scanner, which hands ShelfSense the number without giving the app access to your camera.",
                kind = BannerKind.INFO
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AddOption(icon: ImageVector, title: String, body: String, onClick: () -> Unit) {
    val c = ShelfTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).background(c.tint, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = c.primary)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = c.muted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.muted)
    }
}
