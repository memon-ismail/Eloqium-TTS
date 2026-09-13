package org.eloqium.tts.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.eloqium.tts.engine.LocaleMatcher
import org.eloqium.tts.service.MatchMode
import org.eloqium.tts.service.Settings
import org.eloqium.tts.service.UserDictionary
import org.eloqium.tts.service.UserDictionaryEntry

// =============================================================================
// 1. DICTIONARIES SCREEN (LANGUAGES LIST)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryManagerScreen(
    settings: Settings,
    onOpenLanguage: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val repo = settings.userDictionaryRepository
    var addedLanguages by remember { mutableStateOf(repo.getAddedLanguages()) }
    var showAddLanguageDialog by remember { mutableStateOf(false) }

    fun refreshLanguages() {
        addedLanguages = repo.getAddedLanguages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dictionaries",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" }
                    ) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (addedLanguages.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No languages added yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showAddLanguageDialog = true }
                        ) {
                            Text("Add Language")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    item {
                        PaddingHeader(title = "Languages")
                    }
                    items(addedLanguages) { langDict ->
                        val displayName = LocaleMatcher.ENTRIES.firstOrNull {
                            it.bcp47Tag.equals(langDict.languageTag, ignoreCase = true)
                        }?.displayName ?: langDict.languageTag

                        val count = langDict.dictionaries.size
                        val countLabel = if (count == 1) "1 dictionary" else "$count dictionaries"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenLanguage(langDict.languageTag) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = countLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "›",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { showAddLanguageDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add Language")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddLanguageDialog) {
        val addedTags = addedLanguages.map { it.languageTag.lowercase() }.toSet()
        val availableLanguages = LocaleMatcher.ENTRIES.filter {
            !addedTags.contains(it.bcp47Tag.lowercase())
        }

        AlertDialog(
            onDismissRequest = { showAddLanguageDialog = false },
            title = { Text("Add Language", fontWeight = FontWeight.Bold) },
            text = {
                if (availableLanguages.isEmpty()) {
                    Text("All supported languages have already been added.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                    ) {
                        items(availableLanguages) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        repo.addLanguage(entry.bcp47Tag)
                                        refreshLanguages()
                                        showAddLanguageDialog = false
                                        onOpenLanguage(entry.bcp47Tag)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = entry.displayName, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddLanguageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
// 2. LANGUAGE DICTIONARIES (LIST OF DICTIONARIES IN A LANGUAGE)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LanguageDictionariesScreen(
    settings: Settings,
    languageTag: String,
    onOpenDictionary: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repo = settings.userDictionaryRepository
    var dictionaries by remember { mutableStateOf(repo.getDictionaries(languageTag)) }

    val langDisplayName = LocaleMatcher.ENTRIES.firstOrNull {
        it.bcp47Tag.equals(languageTag, ignoreCase = true)
    }?.displayName ?: languageTag

    // Dialog states
    var showAddDictDialog by remember { mutableStateOf(false) }
    var renameTargetDict by remember { mutableStateOf<UserDictionary?>(null) }
    var deleteTargetDict by remember { mutableStateOf<UserDictionary?>(null) }
    var contextMenuDict by remember { mutableStateOf<UserDictionary?>(null) }
    var exportTargetDict by remember { mutableStateOf<UserDictionary?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshDictionaries() {
        val current = repo.getDictionaries(languageTag)
        dictionaries = current
        if (current.isEmpty()) {
            onNavigateBack()
        }
    }

    // SAF File Importer
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (jsonString != null) {
                    val res = repo.importDictionary(languageTag, jsonString)
                    if (res.isSuccess) {
                        val imported = res.getOrThrow()
                        refreshDictionaries()
                        Toast.makeText(
                            context,
                            "Imported '${imported.dictionary.name}' with ${imported.entryCount} entries",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        errorMessage = "Import failed: ${res.exceptionOrNull()?.message ?: "Invalid JSON format"}"
                    }
                } else {
                    errorMessage = "Failed to read file from storage"
                }
            } catch (t: Throwable) {
                errorMessage = "Import error: ${t.message}"
            }
        }
    }

    // SAF File Exporter
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val target = exportTargetDict
        if (uri != null && target != null) {
            try {
                val exportRes = repo.exportDictionary(languageTag, target.id)
                if (exportRes.isSuccess) {
                    val json = exportRes.getOrThrow()
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                            writer.write(json)
                        }
                    }
                    Toast.makeText(context, "Exported '${target.name}' successfully", Toast.LENGTH_SHORT).show()
                } else {
                    errorMessage = "Export failed: ${exportRes.exceptionOrNull()?.message}"
                }
            } catch (t: Throwable) {
                errorMessage = "Export error: ${t.message}"
            } finally {
                exportTargetDict = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = langDisplayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" }
                    ) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Action buttons: Add Dictionary and Import Dictionary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showAddDictDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Dictionary")
                }
                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Import Dictionary")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                item {
                    PaddingHeader(title = "Dictionaries (${dictionaries.size})")
                }

                items(dictionaries) { dict ->
                    val statusText = if (dict.enabled) "Enabled" else "Disabled"
                    val entryCountText = if (dict.entries.size == 1) "1 entry" else "${dict.entries.size} entries"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onOpenDictionary(dict.id) },
                                onLongClick = { contextMenuDict = dict }
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dict.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$statusText • $entryCountText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (dict.enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = "›",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }

    // Long-press Context Menu Dialog
    if (contextMenuDict != null) {
        val dict = contextMenuDict!!
        val isEnabled = dict.enabled

        AlertDialog(
            onDismissRequest = { contextMenuDict = null },
            title = { Text(dict.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Enable / Disable
                    MenuOptionRow(
                        text = if (isEnabled) "Disable" else "Enable",
                        onClick = {
                            repo.setDictionaryEnabled(languageTag, dict.id, !isEnabled)
                            contextMenuDict = null
                            refreshDictionaries()
                        }
                    )
                    // Rename
                    MenuOptionRow(
                        text = "Rename",
                        onClick = {
                            val target = dict
                            contextMenuDict = null
                            renameTargetDict = target
                        }
                    )
                    // Export
                    MenuOptionRow(
                        text = "Export",
                        onClick = {
                            val target = dict
                            contextMenuDict = null
                            exportTargetDict = target
                            val cleanFileName = "${target.name.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")}_$languageTag.json"
                            exportLauncher.launch(cleanFileName)
                        }
                    )
                    // Delete
                    MenuOptionRow(
                        text = "Delete",
                        isDestructive = true,
                        onClick = {
                            val target = dict
                            contextMenuDict = null
                            deleteTargetDict = target
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { contextMenuDict = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Add Dictionary Dialog
    if (showAddDictDialog) {
        var dictName by remember { mutableStateOf("") }
        var nameError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDictDialog = false },
            title = { Text("Add Dictionary", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = dictName,
                        onValueChange = {
                            dictName = it
                            nameError = false
                        },
                        label = { Text("Dictionary name") },
                        isError = nameError,
                        supportingText = if (nameError) {
                            { Text("Name cannot be empty") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = dictName.trim()
                        if (trimmed.isEmpty()) {
                            nameError = true
                        } else {
                            repo.addDictionary(languageTag, trimmed)
                            showAddDictDialog = false
                            refreshDictionaries()
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDictDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dictionary Dialog
    if (renameTargetDict != null) {
        val target = renameTargetDict!!
        var renameText by remember { mutableStateOf(target.name) }
        var renameError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { renameTargetDict = null },
            title = { Text("Rename Dictionary", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = {
                            renameText = it
                            renameError = false
                        },
                        label = { Text("Dictionary name") },
                        isError = renameError,
                        supportingText = if (renameError) {
                            { Text("Name cannot be empty") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = renameText.trim()
                        if (trimmed.isEmpty()) {
                            renameError = true
                        } else {
                            repo.renameDictionary(languageTag, target.id, trimmed)
                            renameTargetDict = null
                            refreshDictionaries()
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTargetDict = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (deleteTargetDict != null) {
        val target = deleteTargetDict!!

        AlertDialog(
            onDismissRequest = { deleteTargetDict = null },
            title = { Text("Delete dictionary?", fontWeight = FontWeight.Bold) },
            text = { Text("All entries in \"${target.name}\" will be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        repo.deleteDictionary(languageTag, target.id)
                        deleteTargetDict = null
                        refreshDictionaries()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetDict = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error Dialog
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Notice", fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

// =============================================================================
// 3. DICTIONARY ENTRIES (WORDS LIST IN A DICTIONARY)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryEntriesScreen(
    settings: Settings,
    languageTag: String,
    dictionaryId: String,
    onNavigateBack: () -> Unit
) {
    val repo = settings.userDictionaryRepository
    var dict by remember { mutableStateOf(repo.getDictionary(languageTag, dictionaryId)) }
    var entries by remember { mutableStateOf(repo.getEntries(languageTag, dictionaryId)) }

    // Dialog states
    var showAddWordDialog by remember { mutableStateOf(false) }
    var editTargetEntry by remember { mutableStateOf<UserDictionaryEntry?>(null) }
    var deleteTargetEntry by remember { mutableStateOf<UserDictionaryEntry?>(null) }

    fun refreshEntries() {
        val currentDict = repo.getDictionary(languageTag, dictionaryId)
        dict = currentDict
        if (currentDict == null) {
            onNavigateBack()
        } else {
            entries = currentDict.entries
        }
    }

    val currentDict = dict
    if (currentDict == null) {
        onNavigateBack()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentDict.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" }
                    ) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Primary visible action: Add Word
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = { showAddWordDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Word")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No words added yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    item {
                        PaddingHeader(title = "Words and Replacements (${entries.size})")
                    }

                    items(entries) { entry ->
                        val caseLabel = if (entry.caseSensitive) "Case-sensitive" else "Case-insensitive"
                        val matchModeLabel = entry.matchMode.displayName

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editTargetEntry = entry }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${entry.source} → ${entry.replacement}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$matchModeLabel • $caseLabel",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { deleteTargetEntry = entry },
                                modifier = Modifier.semantics { contentDescription = "Delete entry" }
                            ) {
                                Text(
                                    text = "✕",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }

    // Add Word Dialog
    if (showAddWordDialog) {
        WordEntryDialog(
            title = "Add Word",
            initialSource = "",
            initialReplacement = "",
            initialMatchMode = MatchMode.EXACT,
            initialCaseSensitive = false,
            confirmButtonLabel = "Add Word",
            onConfirm = { src, repl, mode, caseSens ->
                repo.addEntry(
                    languageTag,
                    dictionaryId,
                    UserDictionaryEntry(
                        source = src,
                        replacement = repl,
                        matchMode = mode,
                        caseSensitive = caseSens
                    )
                )
                refreshEntries()
                showAddWordDialog = false
            },
            onDismiss = { showAddWordDialog = false }
        )
    }

    // Edit Word Dialog
    if (editTargetEntry != null) {
        val entry = editTargetEntry!!
        WordEntryDialog(
            title = "Edit Word",
            initialSource = entry.source,
            initialReplacement = entry.replacement,
            initialMatchMode = entry.matchMode,
            initialCaseSensitive = entry.caseSensitive,
            confirmButtonLabel = "Save",
            onConfirm = { src, repl, mode, caseSens ->
                repo.updateEntry(
                    languageTag,
                    dictionaryId,
                    entry.copy(
                        source = src,
                        replacement = repl,
                        matchMode = mode,
                        caseSensitive = caseSens
                    )
                )
                refreshEntries()
                editTargetEntry = null
            },
            onDismiss = { editTargetEntry = null }
        )
    }

    // Delete Word Dialog
    if (deleteTargetEntry != null) {
        val entry = deleteTargetEntry!!
        AlertDialog(
            onDismissRequest = { deleteTargetEntry = null },
            title = { Text("Delete entry?", fontWeight = FontWeight.Bold) },
            text = { Text("Remove replacement for '${entry.source}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        repo.deleteEntry(languageTag, dictionaryId, entry.id)
                        deleteTargetEntry = null
                        refreshEntries()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetEntry = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
// 4. WORD ENTRY DIALOG (ADD / EDIT ENTRY)
// =============================================================================

@Composable
fun WordEntryDialog(
    title: String,
    initialSource: String,
    initialReplacement: String,
    initialMatchMode: MatchMode,
    initialCaseSensitive: Boolean,
    confirmButtonLabel: String,
    onConfirm: (source: String, replacement: String, mode: MatchMode, caseSensitive: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var sourceText by remember { mutableStateOf(initialSource) }
    var replacementText by remember { mutableStateOf(initialReplacement) }
    var selectedMode by remember { mutableStateOf(initialMatchMode) }
    var caseSensitive by remember { mutableStateOf(initialCaseSensitive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                // 1. Source text
                OutlinedTextField(
                    value = sourceText,
                    onValueChange = { sourceText = it },
                    label = { Text("Source text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Replacement text
                OutlinedTextField(
                    value = replacementText,
                    onValueChange = { replacementText = it },
                    label = { Text("Replacement text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Match mode",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 3. Match mode choices: EXACT, STARTS_WITH, ENDS_WITH, CONTAINS
                MatchMode.ALL_MODES.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedMode == mode),
                                onClick = { selectedMode = mode },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedMode == mode),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = mode.displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Case sensitivity checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = caseSensitive,
                            onValueChange = { caseSensitive = it },
                            role = Role.Checkbox
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = caseSensitive,
                        onCheckedChange = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Case sensitive", style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = sourceText.trim().isNotEmpty(),
                onClick = {
                    onConfirm(sourceText.trim(), replacementText, selectedMode, caseSensitive)
                }
            ) {
                Text(confirmButtonLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MenuOptionRow(
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PaddingHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .semantics { heading() }
    )
}
