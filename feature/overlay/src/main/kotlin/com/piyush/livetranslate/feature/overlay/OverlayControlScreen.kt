package com.piyush.livetranslate.feature.overlay

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piyush.livetranslate.core.model.LanguageCatalog
import com.piyush.livetranslate.core.ui.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayControlScreen(
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    viewModel: OverlayControlViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var serviceEnabled by remember { mutableStateOf(isOverlayAccessibilityEnabled(context)) }
    var showDisclosure by rememberSaveable { mutableStateOf(!state.settings.overlayConsentGranted) }
    var targetMenu by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) serviceEnabled = isOverlayAccessibilityEnabled(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state.settings.overlayConsentGranted) {
        if (!state.settings.overlayConsentGranted) showDisclosure = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Live screen translation"); Text("Translate without leaving the current app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Settings") }
                    IconButton(onClick = onProfile) { Icon(Icons.Rounded.Person, "Profile") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(16.dp), color = statusColor(serviceEnabled, state.runtime.paused).copy(alpha = .16f)) {
                                Icon(Icons.Rounded.ScreenSearchDesktop, null, Modifier.padding(13.dp), tint = statusColor(serviceEnabled, state.runtime.paused))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(statusTitle(serviceEnabled, state.runtime.paused), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(state.runtime.message, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (!serviceEnabled) {
                            Button(
                                onClick = {
                                    if (!state.settings.overlayConsentGranted) showDisclosure = true
                                    else context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) { Icon(Icons.Rounded.AccessibilityNew, null); Spacer(Modifier.width(8.dp)); Text("Enable accessibility service") }
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = viewModel::togglePause, Modifier.weight(1f)) { Icon(if (state.runtime.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null); Spacer(Modifier.width(6.dp)); Text(if (state.runtime.paused) "Resume" else "Pause") }
                                OutlinedButton(onClick = viewModel::scanOcr, Modifier.weight(1f), enabled = state.settings.overlayOcrFallbackEnabled) { Icon(Icons.Rounded.DocumentScanner, null); Spacer(Modifier.width(6.dp)); Text("OCR scan") }
                            }
                        }
                    }
                }
            }
            if (state.runtime.translation.isNotBlank()) item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(if (state.runtime.mode == DetectionMode.OCR) "OCR fallback" else "Accessible screen text", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(state.runtime.sourcePreview, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.runtime.translation, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            item {
                Text("Translation preferences", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
                Box {
                    ListItem(
                        headlineContent = { Text("Translate into") },
                        supportingContent = { Text(LanguageCatalog.byTag(state.settings.overlayTargetLanguage).displayName) },
                        leadingContent = { Icon(Icons.Rounded.Translate, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Rounded.ExpandMore, null) },
                        modifier = Modifier.clickable { targetMenu = true },
                    )
                    DropdownMenu(targetMenu, { targetMenu = false }) {
                        LanguageCatalog.supported.forEach { language ->
                            DropdownMenuItem(
                                text = { Text("${language.nativeName} · ${language.displayName}") },
                                onClick = { targetMenu = false; viewModel.targetLanguage(language.tag) },
                            )
                        }
                    }
                }
                ListItem(
                    headlineContent = { Text("OCR fallback") },
                    supportingContent = { Text("Use an on-device screenshot only when apps expose no accessible text. Android 11+.") },
                    leadingContent = { Icon(Icons.Rounded.DocumentScanner, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Switch(state.settings.overlayOcrFallbackEnabled, viewModel::ocrFallback) },
                )
                ListItem(
                    headlineContent = { Text("Save overlay translations") },
                    supportingContent = { Text("Off by default for privacy") },
                    leadingContent = { Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Switch(state.settings.overlaySaveHistory, viewModel::saveHistory) },
                )
            }
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("How it works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Step(Icons.Rounded.AccessibilityNew, "Accessibility first", "Only visible, non-editable text is read. Password fields and protected screens are skipped.")
                        Step(Icons.Rounded.AutoAwesome, "New text only", "Changes are deduplicated and debounced to keep translation fast and reduce data use.")
                        Step(Icons.Rounded.PictureInPictureAlt, "Movable overlay", "Translations appear above the app you are using. Drag the 文 bubble, or tap it to collapse.")
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onHistory, Modifier.weight(1f)) { Icon(Icons.Rounded.History, null); Spacer(Modifier.width(6.dp)); Text("History") }
                    if (serviceEnabled) OutlinedButton(onClick = viewModel::stop, Modifier.weight(1f)) { Icon(Icons.Rounded.StopCircle, null); Spacer(Modifier.width(6.dp)); Text("Stop service") }
                }
            }
            if (state.settings.overlayConsentGranted) item {
                TextButton(onClick = viewModel::revokeConsent, Modifier.fillMaxWidth()) { Text("Revoke screen-text consent") }
            }
        }
    }

    if (showDisclosure) AccessibilityDisclosureDialog(
        onDecline = { showDisclosure = false },
        onAccept = {
            viewModel.grantConsent()
            showDisclosure = false
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        },
    )
}

@Composable
private fun AccessibilityDisclosureDialog(onDecline: () -> Unit, onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        icon = { Icon(Icons.Rounded.PrivacyTip, null) },
        title = { Text("Allow live screen-text translation?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("LiveTranslate Pro uses Android Accessibility Service to read visible, non-password text from other apps and show its translation in an overlay.")
                Text("Visible text is sent to your configured translation service. The app never reads password or editable fields and never taps, types, scrolls, or changes another app.")
                Text("If an app exposes no readable text, optional OCR may capture the visible screen on Android 11 or newer. Protected screens cannot be captured. Overlay history is off by default.", fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = { Button(onClick = onAccept) { Text("Allow and open settings") } },
        dismissButton = { TextButton(onClick = onDecline) { Text("Not now") } },
    )
}

@Composable private fun Step(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column { Text(title, fontWeight = FontWeight.SemiBold); Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun statusColor(enabled: Boolean, paused: Boolean): Color = when { !enabled -> MaterialTheme.colorScheme.error; paused -> Color(0xFFFF9800); else -> Color(0xFF2EAD6B) }
private fun statusTitle(enabled: Boolean, paused: Boolean) = when { !enabled -> "Service is off"; paused -> "Translation paused"; else -> "Watching your screen" }
