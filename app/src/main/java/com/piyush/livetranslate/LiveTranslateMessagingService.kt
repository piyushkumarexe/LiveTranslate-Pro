package com.piyush.livetranslate

import android.provider.Settings
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.security.MessageDigest

/** Future-ready FCM entry point. Product-specific notification routing can be added here. */
class LiveTranslateMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        if (FirebaseApp.getApps(this).isEmpty()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val rawId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val deviceId = MessageDigest.getInstance("SHA-256").digest(rawId.toByteArray()).joinToString("") { "%02x".format(it) }.take(24)
        FirebaseFirestore.getInstance().collection("devices").document("${uid}_$deviceId")
            .set(mapOf("fcmToken" to token, "notificationsEnabled" to true), SetOptions.merge())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Reserved for sync nudges and translation feature announcements.
    }
}
