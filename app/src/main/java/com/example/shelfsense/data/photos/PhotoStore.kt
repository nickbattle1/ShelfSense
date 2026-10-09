package com.example.shelfsense.data.photos

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// photos people add to their own items, kept in app storage on this phone.
// cloud photo storage needs Firebase's paid plan, so these deliberately don't sync
class PhotoStore(context: Context) {

    private val appContext = context.applicationContext
    private val folder: File
        get() = File(appContext.filesDir, FOLDER).apply { mkdirs() }

    // an empty file for the camera app to write into
    fun newCaptureFile(): File = File(folder, "${UUID.randomUUID()}.jpg")

    // the camera app can't see app storage directly, so it gets a FileProvider link instead
    fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)

    // the photo picker only grants short term access, so the picked image is copied in
    suspend fun importFrom(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val target = newCaptureFile()
            val input = appContext.contentResolver.openInputStream(uri) ?: return@runCatching null
            input.use { source -> target.outputStream().use { source.copyTo(it) } }
            target.absolutePath
        }.getOrNull()
    }

    fun delete(path: String?) {
        if (path != null) runCatching { File(path).delete() }
    }

    // called on sign out along with the rest of the local data
    fun clearAll() {
        runCatching { File(appContext.filesDir, FOLDER).deleteRecursively() }
    }

    private companion object {
        const val FOLDER = "photos"
    }
}
