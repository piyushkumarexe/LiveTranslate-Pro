package com.piyush.livetranslate.feature.settings

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piyush.livetranslate.core.model.ThemeMode
import com.piyush.livetranslate.core.model.VoiceGender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onPrivacy: () -> Unit, onAbout: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { Section("Appearance") }
            item {
                ChoiceRow(Icons.Rounded.Palette, "Theme", settings.themeMode.name.lowercase().replaceFirstChar(Char::uppercase), ThemeMode.entries.map { it.name to it.name.lowercase().replaceFirstChar(Char::uppercase) }) { viewModel.theme(ThemeMode.valueOf(it)) }
            }
            item { SwitchRow("Dynamic colors", "Match your device wallpaper", settings.dynamicColor, viewModel::dynamicColor) }
            item { HorizontalDivider(Modifier.padding(horizontal = 18.dp)) }
            item { Section("Language & voice") }
            item { ChoiceRow(Icons.Rounded.Language, "App language", if (settings.appLanguage == "hi") "Hindi" else "English", listOf("en" to "English", "hi" to "Hindi"), viewModel::appLanguage) }
            item { ChoiceRow(Icons.Rounded.VolumeUp, "Voice", settings.voiceGender.name.lowercase().replaceFirstChar(Char::uppercase), VoiceGender.entries.map { it.name to it.name.lowercase().replaceFirstChar(Char::uppercase) }) { viewModel.voiceGender(VoiceGender.valueOf(it)) } }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                    Text("Voice speed · ${"%.1f".format(settings.voiceSpeed)}×", fontWeight = FontWeight.SemiBold)
                    Slider(settings.voiceSpeed, viewModel::voiceSpeed, valueRange = .5f..2f, steps = 5)
                }
            }
            item { ActionRow(Icons.Rounded.Download, "Download voice languages", "Manage offline TTS voices") { context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)) } }
            item { HorizontalDivider(Modifier.padding(horizontal = 18.dp)) }
            item { Section("Data & privacy") }
            item { SwitchRow("Cloud sync", "Sync history when signed in", settings.cloudSyncEnabled, viewModel::cloudSync) }
            item { ActionRow(Icons.Rounded.DeleteSweep, "Clear translation cache", "History and favorites are kept", viewModel::clearCache) }
            item { ActionRow(Icons.Rounded.PrivacyTip, "Privacy", "How your data is handled", onPrivacy) }
            item { ActionRow(Icons.Rounded.Info, "About", "Version, credits and licenses", onAbout) }
            item {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Made by Piyush", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("LiveTranslate Pro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable private fun Section(text: String) { Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 6.dp)) }

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle) }, trailingContent = { Switch(checked, onChecked) })
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, action: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
        modifier = Modifier.clickable(onClick = action),
    )
}

@Composable
private fun ChoiceRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, current: String, options: List<Pair<String, String>>, onChoice: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(current) },
            leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
            modifier = Modifier.clickable { open = true },
        )
        DropdownMenu(open, { open = false }) {
            options.forEach { (value, label) -> DropdownMenuItem({ Text(label) }, { open = false; onChoice(value) }) }
        }
    }
}
