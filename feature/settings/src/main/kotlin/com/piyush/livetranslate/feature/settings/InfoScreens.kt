package com.piyush.livetranslate.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.piyush.livetranslate.core.ui.BrandMark
import com.piyush.livetranslate.core.ui.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("About") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark(showCreator = true)
            Spacer(Modifier.height(24.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("Version 1.0.0", fontWeight = FontWeight.SemiBold)
                    Text("Real-time translation for speech, text and images, designed with privacy-aware local history and optional cloud sync.", Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Built with Kotlin, Jetpack Compose, Firebase, Room and Groq through a secure Firebase Functions proxy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Text("© 2026 Piyush · MIT License", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Privacy") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState())) {
            Text("Privacy by design", style = MaterialTheme.typography.headlineMedium)
            PrivacySection("On-device data", "Translation history, favorites, settings and cached results are stored in the app's private Room database. Other apps cannot access it without a compromised device.")
            PrivacySection("Cloud data", "When you sign in and enable cloud sync, profile, device metadata, translations, favorites and settings are stored in your Firebase project and protected by per-user security rules.")
            PrivacySection("Accessibility service", "After a separate prominent disclosure and your affirmative consent, the service reads newly appearing visible text so it can translate over other apps. It skips editable and password fields, known private system surfaces, and never taps, types, scrolls, or changes another app.")
            PrivacySection("OCR fallback", "On Android 11 or newer, optional ML Kit OCR can process a temporary screenshot only when accessible text is unavailable or when you press OCR scan. The overlay is hidden during capture, secure windows cannot be captured, and the bitmap is discarded immediately after recognition.")
            PrivacySection("AI translation", "Recognized text is sent to an authenticated Firebase Function, which forwards it to Groq. The Groq API key never ships in the Android app. Overlay history is disabled by default.")
            PrivacySection("Your controls", "Pause from the movable overlay, stop or disable the service at any time, revoke screen-text consent, keep overlay history off, clear cached translations, disable cloud sync, or sign out.")
        }
    }
}

@Composable private fun PrivacySection(title: String, body: String) {
    Spacer(Modifier.height(20.dp)); Text(title, style = MaterialTheme.typography.titleLarge); Text(body, Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}
