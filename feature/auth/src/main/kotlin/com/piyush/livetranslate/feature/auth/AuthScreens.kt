package com.piyush.livetranslate.feature.auth

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.piyush.livetranslate.core.ui.BrandGradient
import com.piyush.livetranslate.core.ui.BrandMark
import com.piyush.livetranslate.core.ui.GradientSurface
import kotlinx.coroutines.launch

private data class OnboardingPage(val icon: ImageVector, val title: String, val body: String)
private val pages = listOf(
    OnboardingPage(Icons.Rounded.AccessibilityNew, "Translation in every app", "LiveTranslate watches newly appearing visible text through Android Accessibility and translates it without making you switch apps."),
    OnboardingPage(Icons.Rounded.PictureInPictureAlt, "A movable live overlay", "Translations stay above the app you are using. Drag the bubble, pause instantly, or stop the service at any time."),
    OnboardingPage(Icons.Rounded.DocumentScanner, "OCR only when needed", "If an app does not expose accessible text, optional on-device OCR can read the visible screen on Android 11 and newer."),
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    var page by remember { mutableIntStateOf(0) }
    GradientSurface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark()
            Spacer(Modifier.weight(1f))
            AnimatedContent(page, label = "onboarding") { index ->
                val item = pages[index]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(112.dp).background(Brush.linearGradient(BrandGradient), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(item.icon, null, tint = Color.White, modifier = Modifier.size(52.dp)) }
                    Spacer(Modifier.height(28.dp))
                    Text(item.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(item.body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                pages.indices.forEach { index ->
                    Box(Modifier.size(if (index == page) 22.dp else 8.dp, 8.dp).background(if (index == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape))
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (page < pages.lastIndex) page++ else {
                        viewModel.completeOnboarding()
                        onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(if (page == pages.lastIndex) "Continue" else "Next") }
        }
    }
}

@Composable
fun LoginScreen(
    webClientId: String,
    onSignedIn: () -> Unit,
    onContinueOffline: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.user) { if (state.user != null) onSignedIn() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    GradientSurface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BrandMark(showCreator = true)
                Spacer(Modifier.height(34.dp))
                Text("Your world, translated live.", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text("Sign in to sync history and favorites across devices.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(34.dp))
                Button(
                    enabled = !state.loading && webClientId.isNotBlank() && activity != null,
                    onClick = {
                        scope.launch {
                            runCatching { requestGoogleIdToken(requireNotNull(activity), webClientId) }
                                .onSuccess(viewModel::authenticate)
                                .onFailure { snackbar.showSnackbar(it.message ?: "Google sign-in was cancelled.") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Continue with Google", fontWeight = FontWeight.SemiBold)
                }
                if (webClientId.isBlank()) {
                    Text("Google Sign-In will be enabled after adding google-services.json.", Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onContinueOffline, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Preview controls without cloud translation") }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}

private suspend fun requestGoogleIdToken(activity: Activity, webClientId: String): String {
    val option = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = CredentialManager.create(activity).getCredential(activity, request).credential
    require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        "Google returned an unsupported credential."
    }
    return GoogleIdTokenCredential.createFrom(credential.data).idToken
}
