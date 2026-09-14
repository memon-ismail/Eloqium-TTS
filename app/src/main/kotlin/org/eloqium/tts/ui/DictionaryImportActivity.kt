package org.eloqium.tts.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.eloqium.tts.engine.LocaleMatcher
import org.eloqium.tts.service.DictionaryEntryType
import org.eloqium.tts.service.IbmDicImporter
import org.eloqium.tts.service.ImportResult
import org.eloqium.tts.service.Settings
import org.eloqium.tts.service.UserDictionaryJson
import org.eloqium.tts.ui.theme.EloqiumTheme

/**
 * Handles external file open intents from Android File Manager ("Open With" -> "Eloqium Dictionary Importer").
 *
 * Safe import flow:
 * 1. Safely parses and validates the dictionary (JSON or IBM .dic) without modifying persistent state.
 * 2. Displays a confirmation dialog showing dictionary name, proposed language, and allows destination language selection.
 * 3. Shows warnings if target locale does not match detected IBM .dic source language.
 * 4. Only modifies repository state if the user explicitly confirms import.
 * 5. Displays an accessible success or error result.
 */
class DictionaryImportActivity : ComponentActivity() {

    private lateinit var settings: Settings

    sealed class ImportFlowState {
        data class Confirm(
            val dictionaryName: String,
            val initialLanguageTag: String,
            val initialLangDisplayName: String,
            val detectedSourceLanguage: String? = null,
            val entryCount: Int,
            val pronunciationCount: Int,
            val textCount: Int,
            val isIbmDic: Boolean = false,
            val warnings: List<String> = emptyList(),
            val execute: (targetLocale: String) -> ImportFlowState
        ) : ImportFlowState()

        data class Success(val imported: ImportResult, val langDisplayName: String) : ImportFlowState()
        data class Error(val message: String) : ImportFlowState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)

        val uri = extractUriFromIntent(intent)
        val initialFlowState = if (uri != null) {
            validateUri(uri)
        } else {
            ImportFlowState.Error("No dictionary file provided to import.")
        }

        setContent {
            EloqiumTheme {
                DictionaryImportScreen(
                    initialState = initialFlowState,
                    onOpenSettings = {
                        val i = Intent(this, MainActivity::class.java).apply {
                            action = "android.speech.tts.engine.CONFIGURE_ENGINE"
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(i)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun extractUriFromIntent(intent: Intent?): Uri? {
        if (intent == null) return null
        val direct = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri) ?: intent.data
            }
            else -> intent.data
        }
        if (direct != null) return direct

        val clip = intent.clipData
        if (clip != null && clip.itemCount > 0) {
            val itemUri = clip.getItemAt(0)?.uri
            if (itemUri != null) return itemUri
        }

        @Suppress("DEPRECATION")
        val stream = intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        return stream
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            name = it.getString(index)
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
        if (name == null) {
            name = uri.path?.let { p ->
                val cut = p.lastIndexOf('/')
                if (cut != -1) p.substring(cut + 1) else p
            }
        }
        return name
    }

    private fun validateUri(uri: Uri): ImportFlowState {
        return try {
            val bytes = contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            } ?: return ImportFlowState.Error("Could not open file input stream.")

            val filename = getFileName(this, uri) ?: "imported"

            if (filename.endsWith(".dic", ignoreCase = true)) {
                // Parse as IBM .dic
                val parseRes = IbmDicImporter.parse(bytes, filename)
                if (parseRes.isSuccess) {
                    val result = parseRes.getOrThrow()
                    val (detectedCode, proposedLocale, _) = IbmDicImporter.detectLanguage(filename)
                    val langName = LocaleMatcher.ENTRIES.firstOrNull {
                        it.bcp47Tag.equals(result.targetLocale, ignoreCase = true) ||
                        it.bcp47Tag.replace('_', '-').equals(result.targetLocale.replace('_', '-'), ignoreCase = true)
                    }?.displayName ?: result.targetLocale

                    ImportFlowState.Confirm(
                        dictionaryName = result.dictionary.name,
                        initialLanguageTag = result.targetLocale,
                        initialLangDisplayName = langName,
                        detectedSourceLanguage = detectedCode,
                        entryCount = result.dictionary.entries.size,
                        pronunciationCount = result.report.pronunciationCount,
                        textCount = result.report.textCount,
                        isIbmDic = true,
                        warnings = result.report.warnings,
                        execute = { selectedLocale ->
                            val reparseRes = IbmDicImporter.parse(bytes, filename, overrideTargetLocale = selectedLocale)
                            if (reparseRes.isSuccess) {
                                val reparse = reparseRes.getOrThrow()
                                val res = settings.userDictionaryRepository.importParsedDictionary(selectedLocale, reparse.dictionary)
                                if (res.isSuccess) {
                                    val selName = LocaleMatcher.ENTRIES.firstOrNull {
                                        it.bcp47Tag.equals(selectedLocale, ignoreCase = true) ||
                                        it.bcp47Tag.replace('_', '-').equals(selectedLocale.replace('_', '-'), ignoreCase = true)
                                    }?.displayName ?: selectedLocale
                                    ImportFlowState.Success(res.getOrThrow(), selName)
                                } else {
                                    ImportFlowState.Error("Import failed: ${res.exceptionOrNull()?.message ?: "Unknown error"}")
                                }
                            } else {
                                ImportFlowState.Error("Failed to parse dictionary: ${reparseRes.exceptionOrNull()?.message}")
                            }
                        }
                    )
                } else {
                    ImportFlowState.Error("Failed to parse IBM .dic: ${parseRes.exceptionOrNull()?.message}")
                }
            } else {
                // Try JSON first
                val jsonString = String(bytes, Charsets.UTF_8)
                val parsedResult = UserDictionaryJson.parseDictionary(jsonString)
                if (parsedResult.isSuccess) {
                    val parsed = parsedResult.getOrThrow()
                    val langName = LocaleMatcher.ENTRIES.firstOrNull {
                        it.bcp47Tag.equals(parsed.languageTag, ignoreCase = true) ||
                        it.bcp47Tag.replace('_', '-').equals(parsed.languageTag.replace('_', '-'), ignoreCase = true)
                    }?.displayName ?: parsed.languageTag

                    val pronCount = parsed.dictionary.entries.count { it.type == DictionaryEntryType.PRONUNCIATION }
                    val txtCount = parsed.dictionary.entries.size - pronCount

                    ImportFlowState.Confirm(
                        dictionaryName = parsed.dictionary.name,
                        initialLanguageTag = parsed.languageTag,
                        initialLangDisplayName = langName,
                        detectedSourceLanguage = null,
                        entryCount = parsed.dictionary.entries.size,
                        pronunciationCount = pronCount,
                        textCount = txtCount,
                        isIbmDic = false,
                        execute = { selectedLocale ->
                            val res = settings.userDictionaryRepository.importDictionary(selectedLocale, jsonString)
                            if (res.isSuccess) {
                                val selName = LocaleMatcher.ENTRIES.firstOrNull {
                                    it.bcp47Tag.equals(selectedLocale, ignoreCase = true) ||
                                    it.bcp47Tag.replace('_', '-').equals(selectedLocale.replace('_', '-'), ignoreCase = true)
                                }?.displayName ?: selectedLocale
                                ImportFlowState.Success(res.getOrThrow(), selName)
                            } else {
                                ImportFlowState.Error("Import failed: ${res.exceptionOrNull()?.message ?: "Unknown error"}")
                            }
                        }
                    )
                } else {
                    // Fallback to IBM .dic parser if JSON fails
                    val dicRes = IbmDicImporter.parse(bytes, filename)
                    if (dicRes.isSuccess && dicRes.getOrThrow().dictionary.entries.isNotEmpty()) {
                        val result = dicRes.getOrThrow()
                        val (detectedCode, proposedLocale, _) = IbmDicImporter.detectLanguage(filename)
                        val langName = LocaleMatcher.ENTRIES.firstOrNull {
                            it.bcp47Tag.equals(result.targetLocale, ignoreCase = true) ||
                            it.bcp47Tag.replace('_', '-').equals(result.targetLocale.replace('_', '-'), ignoreCase = true)
                        }?.displayName ?: result.targetLocale

                        ImportFlowState.Confirm(
                            dictionaryName = result.dictionary.name,
                            initialLanguageTag = result.targetLocale,
                            initialLangDisplayName = langName,
                            detectedSourceLanguage = detectedCode,
                            entryCount = result.dictionary.entries.size,
                            pronunciationCount = result.report.pronunciationCount,
                            textCount = result.report.textCount,
                            isIbmDic = true,
                            warnings = result.report.warnings,
                            execute = { selectedLocale ->
                                val reparseRes = IbmDicImporter.parse(bytes, filename, overrideTargetLocale = selectedLocale)
                                if (reparseRes.isSuccess) {
                                    val reparse = reparseRes.getOrThrow()
                                    val res = settings.userDictionaryRepository.importParsedDictionary(selectedLocale, reparse.dictionary)
                                    if (res.isSuccess) {
                                        val selName = LocaleMatcher.ENTRIES.firstOrNull {
                                            it.bcp47Tag.equals(selectedLocale, ignoreCase = true) ||
                                            it.bcp47Tag.replace('_', '-').equals(selectedLocale.replace('_', '-'), ignoreCase = true)
                                        }?.displayName ?: selectedLocale
                                        ImportFlowState.Success(res.getOrThrow(), selName)
                                    } else {
                                        ImportFlowState.Error("Import failed: ${res.exceptionOrNull()?.message ?: "Unknown error"}")
                                    }
                                } else {
                                    ImportFlowState.Error("Failed to parse dictionary: ${reparseRes.exceptionOrNull()?.message}")
                                }
                            }
                        )
                    } else {
                        val errorMsg = parsedResult.exceptionOrNull()?.message ?: "Invalid dictionary file format"
                        ImportFlowState.Error(errorMsg)
                    }
                }
            }
        } catch (t: Throwable) {
            ImportFlowState.Error("Failed to read file: ${t.message}")
        }
    }
}

@Composable
fun DictionaryImportScreen(
    initialState: DictionaryImportActivity.ImportFlowState,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var state by remember { mutableStateOf(initialState) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = state) {
            is DictionaryImportActivity.ImportFlowState.Confirm -> {
                var selectedLocaleTag by remember { mutableStateOf(current.initialLanguageTag) }
                var showLocalePicker by remember { mutableStateOf(false) }

                val selectedEntry = LocaleMatcher.ENTRIES.firstOrNull {
                    it.bcp47Tag.equals(selectedLocaleTag, ignoreCase = true) ||
                    it.bcp47Tag.replace('_', '-').equals(selectedLocaleTag.replace('_', '-'), ignoreCase = true)
                }
                val selectedDisplayName = selectedEntry?.displayName ?: selectedLocaleTag

                // Compute active warnings dynamically if target locale is changed
                val activeWarnings = remember(selectedLocaleTag) {
                    val list = current.warnings.toMutableList()
                    if (current.isIbmDic && current.detectedSourceLanguage != null && current.detectedSourceLanguage != "UNKNOWN") {
                        if (!IbmDicImporter.isLocaleCompatible(current.detectedSourceLanguage, selectedLocaleTag)) {
                            val mismatchWarn = "Target language ($selectedDisplayName) does not match detected dictionary language (${current.detectedSourceLanguage}). Phonetic pronunciation tags may sound incorrect in this language."
                            if (!list.contains(mismatchWarn)) {
                                list.add(mismatchWarn)
                            }
                        }
                    }
                    list
                }

                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = {
                        Text(
                            text = "Import Dictionary?",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Dictionary: ${current.dictionaryName}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (current.detectedSourceLanguage != null && current.detectedSourceLanguage != "UNKNOWN") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Detected Language: ${current.detectedSourceLanguage} (Proposed: ${current.initialLangDisplayName})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Destination Language:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { showLocalePicker = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Destination language: $selectedDisplayName. Tap to change." }
                            ) {
                                Text("$selectedDisplayName  ▼")
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Entries: ${current.entryCount} (${current.pronunciationCount} pronunciation, ${current.textCount} text)",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (activeWarnings.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "⚠️ " + activeWarnings.joinToString("\n\n"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                state = current.execute(selectedLocaleTag)
                            }
                        ) {
                            Text("Import")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                    }
                )

                if (showLocalePicker) {
                    AlertDialog(
                        onDismissRequest = { showLocalePicker = false },
                        title = { Text("Select Destination Language", fontWeight = FontWeight.Bold) },
                        text = {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(LocaleMatcher.ENTRIES) { entry ->
                                    val isSelected = entry.bcp47Tag.equals(selectedLocaleTag, ignoreCase = true) ||
                                                     entry.bcp47Tag.replace('_', '-').equals(selectedLocaleTag.replace('_', '-'), ignoreCase = true)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .selectable(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedLocaleTag = entry.bcp47Tag
                                                    showLocalePicker = false
                                                },
                                                role = Role.RadioButton
                                            )
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = null
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = entry.displayName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLocalePicker = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
            is DictionaryImportActivity.ImportFlowState.Success -> {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = {
                        Text(
                            text = "Dictionary Imported",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Dictionary: ${current.imported.dictionary.name}\n" +
                                       "Language: ${current.langDisplayName}\n" +
                                       "Entries: ${current.imported.entryCount}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = onOpenSettings) {
                            Text("Open Settings")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onDismiss) {
                            Text("Done")
                        }
                    }
                )
            }
            is DictionaryImportActivity.ImportFlowState.Error -> {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = {
                        Text(
                            text = "Import Error",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = current.message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = onDismiss) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}
