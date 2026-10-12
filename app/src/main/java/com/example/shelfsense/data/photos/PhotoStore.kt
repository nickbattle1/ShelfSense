package com.example.shelfsense.data.photos

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// photos people add to their own items, kept in app storage on this phone.
// cloud photo storage needs Firebase's paid plan, so these deliberately don't sync.
// each account has its own folder and each photo is named after its item, so signing out
// leaves them alone and signing back in on this phone finds them again
class PhotoStore(context: Context, private val uid: String?) {

    private val appContext = context.applicationContext
    private val root: File get() = File(appContext.filesDir, FOLDER)
    private val folder: File
        get() = File(root, uid ?: NO_ACCOUNT).apply { mkdirs() }

    // an empty file for the camera app to write into, renamed after its item once that's saved
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

    // gives a photo its item's name, replacing any older photo for that item, or removes the
    // item's photo when it was taken off in the form. returns the path to save on the item
    fun attach(itemId: String, path: String?): String? {
        val target = fileFor(itemId)
        if (path == null) {
            target.delete()
            return null
        }
        val source = File(path)
        if (source.absolutePath == target.absolutePath) return target.absolutePath
        if (!source.exists()) return target.takeIf { it.exists() }?.absolutePath
        return if (source.renameTo(target)) target.absolutePath else path
    }

    // the photo saved for an item on this phone, if there is one
    fun existingFor(itemId: String): String? = fileFor(itemId).takeIf { it.exists() }?.absolutePath

    fun delete(path: String?) {
        if (path != null) runCatching { File(path).delete() }
    }

    // only when the account itself is deleted
    fun deleteAccountPhotos() {
        if (uid != null) runCatching { File(root, uid).deleteRecursively() }
    }

    // photos saved before the per account folders can't be matched to an item again, so sign out clears them
    fun clearLooseFiles() {
        runCatching { root.listFiles()?.filter { it.isFile }?.forEach { it.delete() } }
    }

    private fun fileFor(itemId: String) = File(folder, "$itemId.jpg")

    private companion object {
        const val FOLDER = "photos"
        const val NO_ACCOUNT = "no-account"
    }
}
