package com.piyush.livetranslate.feature.translate

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piyush.livetranslate.core.model.LanguageCatalog
import com.piyush.livetranslate.core.ui.Waveform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(onBack: () -> Unit, viewModel: ConversationViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val speech by viewModel.speechState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) viewModel.toggle() }
    fun toggle() {
        if (state.running || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) viewModel.toggle()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Interpreter mode") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CompactLanguagePicker(LanguageCatalog.byTag(state.languageA), LanguageCatalog.supported, Modifier.weight(1f), viewModel::setLanguageA)
                IconButton(viewModel::swap) { Icon(Icons.Rounded.SwapHoriz, "Swap") }
                CompactLanguagePicker(LanguageCatalog.byTag(state.languageB), LanguageCatalog.supported, Modifier.weight(1f), viewModel::setLanguageB)
            }
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (state.personATurn) "Person A • ${LanguageCatalog.byTag(state.languageA).displayName}" else "Person B • ${LanguageCatalog.byTag(state.languageB).displayName}", fontWeight = FontWeight.Bold)
                    Waveform(speech.isListening, speech.rms, Modifier.fillMaxWidth())
                    Text(speech.partialText.ifBlank { if (state.running) "Listening for speech…" else "Press Start to begin a continuous conversation." }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.translating) CircularProgressIndicator(Modifier.padding(8.dp).size(22.dp), strokeWidth = 2.dp)
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }
            LazyColumn(Modifier.weight(1f).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), reverseLayout = true) {
                items(state.messages.reversed()) { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.personA) Arrangement.Start else Arrangement.End) {
                        Surface(shape = RoundedCornerShape(18.dp), color = if (message.personA) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth(.82f)) {
                            Column(Modifier.padding(14.dp)) {
                                Text(message.original, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(message.translated, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            Button(onClick = ::toggle, modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
                Icon(if (state.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.size(8.dp))
                Text(if (state.running) "Stop conversation" else "Start conversation")
            }
        }
    }
}
