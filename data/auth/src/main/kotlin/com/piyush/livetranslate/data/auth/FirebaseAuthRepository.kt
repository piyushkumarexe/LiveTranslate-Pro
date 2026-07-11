package com.piyush.livetranslate.data.auth

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.piyush.livetranslate.core.model.AuthUser
import com.piyush.livetranslate.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : AuthRepository {

    private fun auth(): FirebaseAuth? = if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAuth.getInstance()
    private fun firestore(): FirebaseFirestore? = if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseFirestore.getInstance()

    override val currentUser: Flow<AuthUser?>
        get() {
            val auth = auth() ?: return flowOf(null)
            return callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toModel()) }
                auth.addAuthStateListener(listener)
                trySend(auth.currentUser?.toModel())
                awaitClose { auth.removeAuthStateListener(listener) }
            }
        }

    override fun currentUserId(): String? = auth()?.currentUser?.uid

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<AuthUser> = runCatching {
        require(idToken.isNotBlank()) { "Google returned an empty ID token." }
        val auth = requireNotNull(auth()) {
            "Firebase is not configured. Add app/google-services.json and rebuild the app."
        }
        val result = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        val user = requireNotNull(result.user) { "Firebase did not return a user profile." }
        persistProfile(user.uid, user.displayName.orEmpty(), user.email.orEmpty(), user.photoUrl?.toString())
        user.toModel()
    }

    override suspend fun signOut() {
        auth()?.signOut()
    }

    private suspend fun persistProfile(uid: String, name: String, email: String, photo: String?) {
        val db = firestore() ?: return
        val userRef = db.collection("users").document(uid)
        db.runTransaction { transaction ->
            val existing = transaction.get(userRef)
            val profile = mutableMapOf<String, Any?>(
                "uid" to uid,
                "name" to name,
                "email" to email,
                "profilePhoto" to photo,
                "lastLogin" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (!existing.exists()) profile["createdTime"] = FieldValue.serverTimestamp()
            transaction.set(userRef, profile, SetOptions.merge())
        }.await()

        val deviceId = hashedDeviceId()
        db.collection("devices").document("${uid}_$deviceId").set(
            mapOf(
                "userId" to uid,
                "deviceId" to deviceId,
                "manufacturer" to Build.MANUFACTURER,
                "model" to Build.MODEL,
                "os" to "Android ${Build.VERSION.RELEASE}",
                "sdk" to Build.VERSION.SDK_INT,
                "appVersion" to appVersion(),
                "lastActive" to FieldValue.serverTimestamp(),
                "notificationsEnabled" to false,
            ),
            SetOptions.merge(),
        ).await()
    }

    private fun hashedDeviceId(): String {
        val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }.take(24)
    }

    private fun appVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("unknown")
}

private fun com.google.firebase.auth.FirebaseUser.toModel() = AuthUser(
    uid = uid,
    name = displayName.orEmpty(),
    email = email.orEmpty(),
    photoUrl = photoUrl?.toString(),
)
