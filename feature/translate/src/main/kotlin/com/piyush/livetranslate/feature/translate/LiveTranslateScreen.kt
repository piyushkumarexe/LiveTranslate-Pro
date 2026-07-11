package com.piyush.livetranslate.feature.translate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piyush.livetranslate.core.model.LanguageCatalog
import com.piyush.livetranslate.core.model.LanguageOption
import com.piyush.livetranslate.core.ui.BrandGradient
import com.piyush.livetranslate.core.ui.GlassCard
import com.piyush.livetranslate.core.ui.Waveform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTranslateScreen(onBack: () -> Unit, viewModel: LiveTranslateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val speech by viewModel.speechState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) viewModel.toggleListening() }

    fun toggleSpeech() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) viewModel.toggleListening()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Live translation") },
            navigationIcon = { IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CompactLanguagePicker(LanguageCatalog.byTag(state.sourceLanguage), LanguageCatalog.all(), Modifier.weight(1f), viewModel::setSource)
                IconButton(viewModel::swap) { Icon(Icons.Rounded.SwapVert, "Swap") }
                CompactLanguagePicker(LanguageCatalog.byTag(state.targetLanguage), LanguageCatalog.supported, Modifier.weight(1f), viewModel::setTarget)
            }
            Spacer(Modifier.height(16.dp))
            Waveform(speech.isListening, speech.rms, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.size(100.dp).clip(CircleShape).background(Brush.linearGradient(BrandGradient)).clickable(onClick = ::toggleSpeech),
                contentAlignment = Alignment.Center,
            ) { Icon(if (speech.isListening) Icons.Rounded.Pause else Icons.Rounded.Mic, if (speech.isListening) "Stop" else "Listen", tint = Color.White, modifier = Modifier.size(42.dp)) }
            Text(if (speech.isListening) "Listening…" else "Tap to speak", Modifier.padding(10.dp), fontWeight = FontWeight.SemiBold)
            speech.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(8.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("Recognized text", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = state.recognizedText,
                        onValueChange = viewModel::setInput,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        placeholder = { Text("Speak or type here…") },
                        minLines = 3,
                    )
                    Button(onClick = { viewModel.translateTyped() }, enabled = !state.loading && state.recognizedText.isNotBlank(), modifier = Modifier.align(Alignment.End).padding(top = 10.dp)) { Text("Translate") }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .48f)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Translation", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    Text(state.translatedText.ifBlank { "Your translation appears here." }, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 18.dp))
                    state.confidence?.let {
                        Text("Confidence ${(it * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                        LinearProgressIndicator(progress = { it.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                    }
                    state.detectedLanguage?.let { Text("Detected: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton({ clipboard.setText(AnnotatedString(state.translatedText)) }, enabled = state.translatedText.isNotBlank()) { Icon(Icons.Rounded.ContentCopy, "Copy") }
                        IconButton({
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, state.translatedText) }, "Share translation"))
                        }, enabled = state.translatedText.isNotBlank()) { Icon(Icons.Rounded.Share, "Share") }
                        IconButton(viewModel::favoriteLatest, enabled = state.translatedText.isNotBlank()) { Icon(Icons.Rounded.FavoriteBorder, "Favorite") }
                        IconButton(viewModel::replay, enabled = state.translatedText.isNotBlank()) { Icon(Icons.Rounded.VolumeUp, "Replay") }
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
internal fun CompactLanguagePicker(selected: LanguageOption, options: List<LanguageOption>, modifier: Modifier = Modifier, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { expanded = true }.padding(10.dp)) {
            Text(selected.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(selected.nativeName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text("${option.nativeName} · ${option.displayName}") }, onClick = { expanded = false; onSelected(option.tag) }) }
        }
    }
}
