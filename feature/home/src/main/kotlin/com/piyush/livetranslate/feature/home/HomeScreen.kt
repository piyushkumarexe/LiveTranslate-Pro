package com.piyush.livetranslate.feature.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.InterpreterMode
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piyush.livetranslate.core.model.LanguageCatalog
import com.piyush.livetranslate.core.model.LanguageOption
import com.piyush.livetranslate.core.ui.BrandGradient
import com.piyush.livetranslate.core.ui.GlassCard
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLiveTranslate: (String, String) -> Unit,
    onConversation: () -> Unit,
    onOcr: () -> Unit,
    onHistory: () -> Unit,
    onFavorites: () -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    var source by rememberSaveable { mutableStateOf("auto") }
    var target by rememberSaveable { mutableStateOf("en-US") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hello${user?.name?.substringBefore(' ')?.takeIf { it.isNotBlank() }?.let { ", $it" }.orEmpty()}", style = MaterialTheme.typography.titleLarge)
                        Text("What should we translate?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Settings") }
                    IconButton(onClick = onProfile) { Icon(Icons.Rounded.Person, "Profile") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            LanguagePicker(LanguageCatalog.byTag(source), LanguageCatalog.all(), Modifier.weight(1f)) { source = it.tag }
                            IconButton(onClick = {
                                if (source != "auto") {
                                    val old = source; source = target; target = old
                                }
                            }) { Icon(Icons.Rounded.SwapVert, "Swap languages") }
                            LanguagePicker(LanguageCatalog.byTag(target), LanguageCatalog.supported, Modifier.weight(1f)) { target = it.tag }
                        }
                        Spacer(Modifier.height(24.dp))
                        Box(
                            Modifier.size(126.dp).clip(CircleShape).background(Brush.linearGradient(BrandGradient)).clickable { onLiveTranslate(source, target) },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Mic, "Start live translation", tint = Color.White, modifier = Modifier.size(56.dp)) }
                        Spacer(Modifier.height(14.dp))
                        Text("Tap to speak", fontWeight = FontWeight.SemiBold)
                        Text("Live subtitles • low latency", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Shortcut(Icons.Rounded.InterpreterMode, "Conversation", onConversation, Modifier.weight(1f))
                    Shortcut(Icons.Rounded.CameraAlt, "Camera", onOcr, Modifier.weight(1f))
                    Shortcut(Icons.Rounded.History, "History", onHistory, Modifier.weight(1f))
                    Shortcut(Icons.Rounded.Favorite, "Favorites", onFavorites, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent translations", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Text("View all", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onHistory).padding(8.dp))
                }
            }
            if (recent.isEmpty()) item {
                Text("Your translations will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
            }
            items(recent.take(5), key = { it.id }) { item ->
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.sourceText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.translatedText, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.KeyboardArrowRight, null)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Shortcut(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickable(onClick = onClick).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon, label, Modifier.padding(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun LanguagePicker(selected: LanguageOption, options: List<LanguageOption>, modifier: Modifier = Modifier, onSelected: (LanguageOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { expanded = true }.padding(10.dp)) {
            Text(if (selected.tag == "auto") "SOURCE" else selected.tag.substringBefore('-').uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(selected.nativeName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { language ->
                DropdownMenuItem(text = { Text("${language.nativeName} · ${language.displayName}") }, onClick = { expanded = false; onSelected(language) })
            }
        }
    }
}
