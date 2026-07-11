package com.piyush.livetranslate.feature.translate

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piyush.livetranslate.core.model.LanguageCatalog
import com.piyush.livetranslate.core.ui.GlassCard
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(onBack: () -> Unit, viewModel: OcrViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { uri -> viewModel.process(context, uri) } }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) cameraUri?.let { viewModel.process(context, it) } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { cameraUri = createImageUri(context); camera.launch(requireNotNull(cameraUri)) }
    }
    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraUri = createImageUri(context); camera.launch(requireNotNull(cameraUri))
        } else permission.launch(Manifest.permission.CAMERA)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("OCR translation") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = ::openCamera, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.CameraAlt, null); Spacer(Modifier.padding(4.dp)); Text("Camera") }
                OutlinedButton(onClick = { gallery.launch("image/*") }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.PhotoLibrary, null); Spacer(Modifier.padding(4.dp)); Text("Gallery") }
            }
            Spacer(Modifier.height(14.dp))
            CompactLanguagePicker(LanguageCatalog.byTag(state.targetLanguage), LanguageCatalog.supported, Modifier.fillMaxWidth(), viewModel::setTarget)
            Spacer(Modifier.height(10.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("Recognized text", color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(state.recognizedText, viewModel::setText, Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 5, placeholder = { Text("Capture or select an image.") })
                    Button(onClick = viewModel::translateCurrent, enabled = state.recognizedText.isNotBlank() && !state.loading, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) { Text("Translate") }
                }
            }
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Translation", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        if (state.loading) CircularProgressIndicator(Modifier.padding(2.dp), strokeWidth = 2.dp)
                    }
                    Text(state.translatedText.ifBlank { "Translated text appears here." }, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 16.dp))
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        }
    }
}

private fun createImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File.createTempFile("ocr_", ".jpg", directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}
