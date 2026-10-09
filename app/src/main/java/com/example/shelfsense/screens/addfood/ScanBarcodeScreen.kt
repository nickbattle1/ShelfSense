package com.example.shelfsense.screens.addfood

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.shelfsense.data.scanner.BarcodeScanner
import com.example.shelfsense.data.scanner.ScanOutcome
import com.example.shelfsense.ui.components.PrimaryButton
import com.example.shelfsense.ui.components.ScreenHeader
import com.example.shelfsense.ui.components.SecondaryButton
import com.example.shelfsense.ui.theme.ShelfTheme
import kotlinx.coroutines.launch

private sealed interface ScanUi {
    data object Idle : ScanUi
    data class Preparing(val progress: Int?) : ScanUi
    data object Scanning : ScanUi
    data object Cancelled : ScanUi
    data class Failed(val message: String) : ScanUi
}

@Composable
fun ScanBarcodeScreen(
    onBack: () -> Unit,
    onScanned: (String) -> Unit,
    onTypeInstead: () -> Unit
) {
    val c = ShelfTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scanner = remember(context) { BarcodeScanner(context) }
    var ui by remember { mutableStateOf<ScanUi>(ScanUi.Idle) }
    var autoStarted by rememberSaveable { mutableStateOf(false) }
    val busy = ui is ScanUi.Preparing || ui is ScanUi.Scanning

    val startScan: () -> Unit = {
        scope.launch {
            ui = ScanUi.Preparing(null)
            val ready = scanner.prepare { progress -> ui = ScanUi.Preparing(progress) }
            if (!ready) {
                ui = ScanUi.Failed("The scanner couldn't be set up on this device. You can type the number instead.")
                return@launch
            }
            ui = ScanUi.Scanning
            ui = when (val outcome = scanner.scan()) {
                is ScanOutcome.Scanned -> {
                    onScanned(outcome.code)
                    ScanUi.Idle
                }
                ScanOutcome.Cancelled -> ScanUi.Cancelled
                is ScanOutcome.Failed -> ScanUi.Failed(outcome.message)
            }
        }
    }

    // opens the scanner straight away the first time, but not again when coming back from the form
    LaunchedEffect(Unit) {
        if (!autoStarted) {
            autoStarted = true
            startScan()
        }
    }

    Scaffold(containerColor = c.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            ScreenHeader(title = "Scan barcode", onBack = onBack)
            Viewfinder(
                ui = ui,
                enabled = !busy,
                onTap = startScan,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                statusText(ui),
                style = MaterialTheme.typography.bodyMedium,
                color = if (ui is ScanUi.Failed) c.urgent else c.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
            Spacer(Modifier.height(16.dp))
            if (!busy) {
                PrimaryButton(if (ui is ScanUi.Idle) "Start scanning" else "Scan again", onClick = startScan)
                Spacer(Modifier.height(10.dp))
            }
            SecondaryButton("Enter the number instead", onClick = onTypeInstead)
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun statusText(ui: ScanUi): String = when (ui) {
    ScanUi.Idle -> "Hold the package steady until the number is read."
    is ScanUi.Preparing ->
        if (ui.progress == null) "Getting the scanner ready…" else "Downloading the scanner, ${ui.progress}%"
    ScanUi.Scanning -> "Point the camera at the barcode"
    ScanUi.Cancelled -> "Scanning was cancelled. Try again, or type the number instead."
    is ScanUi.Failed -> ui.message
}

// the dark viewfinder panel from the prototype, now with a moving scan line
@Composable
private fun Viewfinder(ui: ScanUi, enabled: Boolean, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val c = ShelfTheme.colors
    val transition = rememberInfiniteTransition(label = "scan line")
    val sweep by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "sweep"
    )
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.scanner)
            .clickable(enabled = enabled, onClickLabel = "Start scanning", onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.78f)
                .height(190.dp)
                .drawBehind {
                    val arm = 28.dp.toPx()
                    val stroke = 4.dp.toPx()
                    val w = size.width
                    val h = size.height
                    val ink = c.scannerInk
                    // corner brackets
                    drawLine(ink, Offset(0f, 0f), Offset(arm, 0f), stroke, StrokeCap.Round)
                    drawLine(ink, Offset(0f, 0f), Offset(0f, arm), stroke, StrokeCap.Round)
                    drawLine(ink, Offset(w, 0f), Offset(w - arm, 0f), stroke, StrokeCap.Round)
                    drawLine(ink, Offset(w, 0f), Offset(w, arm), stroke, StrokeCap.Round)
                    drawLine(ink, Offset(0f, h), Offset(arm, h), stroke, StrokeCap.Round)
                    drawLine(ink, Offset(0f, h), Offset(0f, h - arm), stroke, StrokeCap.Round)
                    drawLine(ink, Offset(w, h), Offset(w - arm, h), stroke, StrokeCap.Round)
                    drawLine(ink, Offset(w, h), Offset(w, h - arm), stroke, StrokeCap.Round)
                    val y = h * sweep
                    drawLine(c.sage, Offset(10.dp.toPx(), y), Offset(w - 10.dp.toPx(), y), 2.dp.toPx())
                }
        )
        when (ui) {
            is ScanUi.Preparing -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val progress = ui.progress
                if (progress == null) {
                    CircularProgressIndicator(color = c.scannerInk)
                } else {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.width(160.dp),
                        color = c.scannerInk,
                        trackColor = c.scannerInk.copy(alpha = 0.25f)
                    )
                }
            }
            else -> Icon(
                Icons.Filled.QrCodeScanner,
                contentDescription = null,
                tint = c.scannerInk.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp)
            )
        }
    }
}
