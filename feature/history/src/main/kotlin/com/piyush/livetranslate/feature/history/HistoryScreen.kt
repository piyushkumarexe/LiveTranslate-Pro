package com.piyush.livetranslate.feature.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piyush.livetranslate.core.ui.EmptyState
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(favoritesOnly: Boolean, onBack: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val items = if (favoritesOnly) favorites else history
    var query by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var csv by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer -> writer.write(csv) } }
    }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (favoritesOnly) "Favorites" else "History") },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                actions = {
                    if (!favoritesOnly) {
                        IconButton(viewModel::sync) { Icon(Icons.Rounded.CloudSync, "Sync now") }
                        IconButton({ scope.launch { csv = viewModel.exportCsv(); export.launch("livetranslate-history.csv") } }) { Icon(Icons.Rounded.Download, "Export CSV") }
                        IconButton({ confirmClear = true }) { Icon(Icons.Rounded.Delete, "Clear history") }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (!favoritesOnly) {
                OutlinedTextField(
                    query,
                    { query = it; viewModel.search(it) },
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    placeholder = { Text("Search translations") },
                    singleLine = true,
                )
            }
            if (items.isEmpty()) EmptyState(if (favoritesOnly) "No favorites yet" else "No history yet", if (favoritesOnly) "Tap the heart on a translation to keep it here." else "Translations you save will appear here.")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { item ->
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(item.sourceText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(item.translatedText, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                            Row(Modifier.fillMaxWidth()) {
                                Text("${item.sourceLanguage} → ${item.targetLanguage} · ${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(item.createdAt))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(top = 12.dp))
                                IconButton({ viewModel.toggleFavorite(item.id, !item.isFavorite) }) { Icon(if (item.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Favorite", tint = if (item.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                                IconButton({ viewModel.delete(item.id) }) { Icon(Icons.Rounded.Delete, "Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Clear translation history?") },
        text = { Text("This removes local history and synced cloud records. This action cannot be undone.") },
        confirmButton = { TextButton({ confirmClear = false; viewModel.clear() }) { Text("Clear") } },
        dismissButton = { TextButton({ confirmClear = false }) { Text("Cancel") } },
    )
}
