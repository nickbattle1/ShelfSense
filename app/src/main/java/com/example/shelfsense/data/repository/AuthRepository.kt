package com.example.shelfsense.data.repository

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.example.shelfsense.data.photos.PhotoStore
import com.example.shelfsense.worker.WorkScheduler
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

// Firebase email and password auth. profile details go to DataStore first and reach
// Firestore through the sync worker, the same offline first path as pantry items
class AuthRepository(context: Context) {

    private val appContext = context.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val settings = SettingsRepository(appContext)

    val currentUser: FirebaseUser? get() = auth.currentUser

    // the main app needs a signed in user with a confirmed email
    val hasVerifiedUser: Boolean get() = auth.currentUser?.isEmailVerified == true

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        // pulls the account's pantry down, which is how a second device gets the same items
        WorkScheduler.requestSync(appContext, pull = true)
    }

    suspend fun signUp(name: String, email: String, password: String, household: String) {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
        user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())?.await()
        // a failed send isn't fatal, the verify screen can always send another
        runCatching { user?.sendEmailVerification()?.await() }
        settings.saveProfile(name, household, pending = true)
        WorkScheduler.requestSync(appContext)
    }

    suspend fun sendVerification() {
        auth.currentUser?.sendEmailVerification()?.await()
    }

    // the verified flag only updates after a reload, e.g. when the person comes back from their inbox
    suspend fun refreshVerified(): Boolean {
        val user = auth.currentUser ?: return false
        runCatching { user.reload().await() }
        return hasVerifiedUser
    }

    // an unknown email still reports success, so the screen never reveals which addresses have accounts
    suspend fun sendPasswordReset(email: String) {
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
        } catch (e: FirebaseAuthInvalidUserException) {
            // nothing to send, the person sees the same confirmation either way
        }
    }

    suspend fun updateProfile(name: String, household: String) {
        settings.saveProfile(name, household, pending = true)
        WorkScheduler.requestSync(appContext)
        // best effort only, the app reads the name back from DataStore anyway
        runCatching {
            auth.currentUser
                ?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                ?.await()
        }
    }

    // the pantry is backed up in Firestore, so the local copy is cleared and pulled again on next sign in
    suspend fun signOut() {
        WorkScheduler.cancelSync(appContext)
        auth.signOut()
        clearDeviceData()
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        reauthenticate(currentPassword).updatePassword(newPassword).await()
    }

    // the cloud copy goes first while the account can still reach it, then the account, then this phone's copy.
    // if the account delete fails, a retry finds no cloud data left and simply finishes the job
    suspend fun deleteAccount(currentPassword: String) {
        val user = reauthenticate(currentPassword)
        WorkScheduler.cancelSync(appContext)
        PantryRepository(appContext).deleteCloudData(user.uid)
        user.delete().await()
        auth.signOut()
        clearDeviceData()
    }

    // Firebase only allows a password change or account deletion straight after signing in,
    // so both ask for the current password and sign in again first
    private suspend fun reauthenticate(currentPassword: String): FirebaseUser {
        val user = auth.currentUser ?: throw IllegalStateException("No one is signed in")
        val email = user.email ?: throw IllegalStateException("This account has no email address")
        user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
        return user
    }

    // everything this phone holds for the account, cleared on sign out and after deleting it
    private suspend fun clearDeviceData() {
        PantryRepository(appContext).clearLocal()
        settings.clearAccount()
        PhotoStore(appContext).clearAll()
        NotificationManagerCompat.from(appContext).cancelAll()
    }

    companion object {
        // Firebase exceptions turned into messages a person can act on. with email enumeration
        // protection on, a wrong password and an unknown email both arrive as invalid credentials
        fun messageFor(error: Throwable): String = when (error) {
            is FirebaseNetworkException -> "No internet connection. Check your connection and try again."
            is FirebaseTooManyRequestsException -> "Too many attempts. Wait a minute and try again."
            is FirebaseAuthWeakPasswordException ->
                "That password is too weak. Use at least 8 characters, including a number."
            is FirebaseAuthUserCollisionException ->
                "An account already exists for that email. Try logging in instead."
            is FirebaseAuthInvalidUserException ->
                if (error.errorCode == "ERROR_USER_DISABLED") {
                    "This account has been disabled."
                } else {
                    "Email or password is incorrect."
                }
            is FirebaseAuthInvalidCredentialsException ->
                if (error.errorCode == "ERROR_INVALID_EMAIL") {
                    "That email address doesn't look right."
                } else {
                    "Email or password is incorrect."
                }
            is FirebaseFirestoreException ->
                if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    "You're offline. Connect to the internet and try again."
                } else {
                    "Something went wrong. Please try again."
                }
            else -> "Something went wrong. Please try again."
        }

        // after re-entering a password, a bad credential can only mean that password was wrong
        fun reauthMessageFor(error: Throwable): String =
            if (error is FirebaseAuthInvalidCredentialsException) {
                "That password isn't right. Try again."
            } else {
                messageFor(error)
            }
    }
}
