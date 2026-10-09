package com.example.shelfsense.data.scanner

import android.content.Context
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

sealed interface ScanOutcome {
    data class Scanned(val code: String) : ScanOutcome
    data object Cancelled : ScanOutcome
    data class Failed(val message: String) : ScanOutcome
}

// wraps the Google code scanner. Play services owns the camera screen,
// so ShelfSense never has to ask for the camera permission itself
class BarcodeScanner(private val context: Context) {

    private val scanner: GmsBarcodeScanner by lazy {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    // the scanner ships as an on demand module. on a fresh emulator it isn't downloaded yet,
    // so this installs it first and reports progress rather than failing on the first scan
    suspend fun prepare(onProgress: (Int?) -> Unit): Boolean {
        val client = ModuleInstall.getClient(context)
        return try {
            if (client.areModulesAvailable(scanner).await().areModulesAvailable()) return true
            suspendCancellableCoroutine { continuation ->
                val listener = object : InstallStatusListener {
                    override fun onInstallStatusUpdated(update: ModuleInstallStatusUpdate) {
                        update.progressInfo?.let { info ->
                            val total = info.totalBytesToDownload
                            onProgress(if (total > 0) (info.bytesDownloaded * 100 / total).toInt() else null)
                        }
                        val finished = when (update.installState) {
                            ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED -> true
                            ModuleInstallStatusUpdate.InstallState.STATE_FAILED,
                            ModuleInstallStatusUpdate.InstallState.STATE_CANCELED -> false
                            else -> null
                        }
                        if (finished != null) {
                            client.unregisterListener(this)
                            if (continuation.isActive) continuation.resume(finished)
                        }
                    }
                }
                val request = ModuleInstallRequest.newBuilder()
                    .addApi(scanner)
                    .setListener(listener)
                    .build()
                client.installModules(request)
                    .addOnSuccessListener { response ->
                        if (response.areModulesAlreadyInstalled()) {
                            client.unregisterListener(listener)
                            if (continuation.isActive) continuation.resume(true)
                        }
                    }
                    .addOnFailureListener {
                        client.unregisterListener(listener)
                        if (continuation.isActive) continuation.resume(false)
                    }
                continuation.invokeOnCancellation { client.unregisterListener(listener) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    suspend fun scan(): ScanOutcome = suspendCancellableCoroutine { continuation ->
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val code = barcode.rawValue
                val outcome = if (code.isNullOrBlank()) {
                    ScanOutcome.Failed("That barcode couldn't be read. Try again or type the number instead.")
                } else {
                    ScanOutcome.Scanned(code)
                }
                if (continuation.isActive) continuation.resume(outcome)
            }
            .addOnCanceledListener {
                if (continuation.isActive) continuation.resume(ScanOutcome.Cancelled)
            }
            .addOnFailureListener { error ->
                val message = error.message ?: "The scanner stopped unexpectedly."
                if (continuation.isActive) continuation.resume(ScanOutcome.Failed(message))
            }
    }
}
