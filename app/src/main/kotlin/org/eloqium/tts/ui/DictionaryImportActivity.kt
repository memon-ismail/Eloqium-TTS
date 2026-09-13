package org.eloqium.tts.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.eloqium.tts.engine.LocaleMatcher
import org.eloqium.tts.service.ImportResult
import org.eloqium.tts.service.ParsedDictionary
import org.eloqium.tts.service.Settings
import org.eloqium.tts.service.UserDictionaryJson
import org.eloqium.tts.ui.theme.EloqiumTheme

/**
 * Handles external file open intents from Android File Manager ("Open With" -> "Eloqium Dictionary Importer").
 *
 * Safe import flow:
 * 1. Safely parses and validates the JSON without modifying persistent state.
 * 2. Displays a confirmation dialog showing dictionary name, language, and entry count.
 * 3. Only modifies repository state if the user explicitly confirms import.
 * 4. Displays an accessible success or error result.
 */
class DictionaryImportActivity : ComponentActivity() {

    private lateinit var settings: Settings

    sealed class ImportFlowState {
        data class Confirm(val parsed: ParsedDictionary, val langDisplayName: String, val rawJson: String) : ImportFlowState()
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
                    onExecuteImport = { rawJson ->
                        val result = settings.userDictionaryRepository.importDictionary(null, rawJson)
                        if (result.isSuccess) {
                            val imp = result.getOrThrow()
                            val langName = LocaleMatcher.ENTRIES.firstOrNull {
                                it.bcp47Tag.equals(imp.languageTag, ignoreCase = true) ||
                                it.bcp47Tag.replace('_', '-').equals(imp.languageTag.replace('_', '-'), ignoreCase = true)
                            }?.displayName ?: imp.languageTag
                            ImportFlowState.Success(imp, langName)
                        } else {
                            ImportFlowState.Error("Import failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}")
                        }
                    },
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

    private fun validateUri(uri: Uri): ImportFlowState {
        return try {
            val jsonString = contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return ImportFlowState.Error("Could not open file input stream.")

            val parsedResult = UserDictionaryJson.parseDictionary(jsonString)
            if (parsedResult.isSuccess) {
                val parsed = parsedResult.getOrThrow()
                val langName = LocaleMatcher.ENTRIES.firstOrNull {
                    it.bcp47Tag.equals(parsed.languageTag, ignoreCase = true) ||
                    it.bcp47Tag.replace('_', '-').equals(parsed.languageTag.replace('_', '-'), ignoreCase = true)
                }?.displayName ?: parsed.languageTag
                ImportFlowState.Confirm(parsed, langName, jsonString)
            } else {
                val errorMsg = parsedResult.exceptionOrNull()?.message ?: "Invalid dictionary JSON format"
                ImportFlowState.Error(errorMsg)
            }
        } catch (t: Throwable) {
            ImportFlowState.Error("Failed to read file: ${t.message}")
        }
    }
}

@Composable
fun DictionaryImportScreen(
    initialState: DictionaryImportActivity.ImportFlowState,
    onExecuteImport: (String) -> DictionaryImportActivity.ImportFlowState,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var state by remember { mutableStateOf(initialState) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = state) {
            is DictionaryImportActivity.ImportFlowState.Confirm -> {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = {
                        Text(
                            text = "Import dictionary?",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Dictionary: ${current.parsed.dictionary.name}\n" +
                                       "Language: ${current.langDisplayName}\n" +
                                       "Entries: ${current.parsed.dictionary.entries.size}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                state = onExecuteImport(current.rawJson)
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
            }
            is DictionaryImportActivity.ImportFlowState.Success -> {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    title = {
                        Text(
                            text = "Dictionary imported",
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
                            text = "Import error",
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
