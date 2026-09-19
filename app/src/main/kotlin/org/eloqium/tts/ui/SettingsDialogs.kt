package org.eloqium.tts.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.eloqium.tts.engine.Eci
import org.eloqium.tts.engine.LocaleMatcher
import org.eloqium.tts.service.SettingsDefaults

@Composable
fun VoiceProfileDialog(
    visible: Boolean,
    currentProfile: Int,
    onProfileSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Voice Profile", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                items(Eci.PRESET_NAMES.indices.toList()) { index ->
                    val name = Eci.PRESET_NAMES[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (currentProfile == index),
                                onClick = { onProfileSelected(index) },
                                role = Role.RadioButton
                            )
                            .semantics(mergeDescendants = true) {}
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentProfile == index),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PunctuationLevelDialog(
    visible: Boolean,
    currentLevel: Int,
    customPunctuation: String,
    levels: List<String>,
    onConfirm: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var selectedLevel by remember(visible, currentLevel) { mutableIntStateOf(currentLevel) }
    var customText by remember(visible, customPunctuation) { mutableStateOf(customPunctuation) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(selectedLevel) {
        if (selectedLevel == SettingsDefaults.PUNCT_CUSTOM) {
            try {
                delay(50)
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Punctuation Level", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                levels.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedLevel == index),
                                onClick = { selectedLevel = index },
                                role = Role.RadioButton
                            )
                            .semantics(mergeDescendants = true) {}
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedLevel == index),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }

                    if (index == SettingsDefaults.PUNCT_CUSTOM && selectedLevel == SettingsDefaults.PUNCT_CUSTOM) {
                        OutlinedTextField(
                            value = customText,
                            onValueChange = { customText = it },
                            label = { Text("Punctuation characters") },
                            placeholder = { Text("e.g. @#%*") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 40.dp, end = 8.dp, bottom = 12.dp)
                                .focusRequester(focusRequester)
                                .semantics {
                                    contentDescription = "Custom punctuation characters to speak"
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedLevel, customText) }) {
                Text("OK")
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
fun NumberProcessingDialog(
    visible: Boolean,
    currentMode: Int,
    modes: List<String>,
    onModeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Number Processing Mode", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (currentMode == index),
                                onClick = { onModeSelected(index) },
                                role = Role.RadioButton
                            )
                            .semantics(mergeDescendants = true) {}
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentMode == index),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LanguageDialog(
    visible: Boolean,
    currentLanguageTag: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                items(LocaleMatcher.ENTRIES) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (currentLanguageTag == entry.bcp47Tag),
                                onClick = { onLanguageSelected(entry.bcp47Tag) },
                                role = Role.RadioButton
                            )
                            .semantics(mergeDescendants = true) {}
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentLanguageTag == entry.bcp47Tag),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = entry.displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SamplingRateDialog(
    visible: Boolean,
    currentRate: Int,
    onRateSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Sampling Rate", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsDefaults.SUPPORTED_SAMPLING_RATES.forEach { rate ->
                    val label = when (rate) {
                        11025 -> "11025 Hz (Default)"
                        else -> "$rate Hz"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (currentRate == rate),
                                onClick = { onRateSelected(rate) },
                                role = Role.RadioButton
                            )
                            .semantics(mergeDescendants = true) {}
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentRate == rate),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ResetConfirmationDialog(
    visible: Boolean,
    onConfirmReset: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset all settings?", fontWeight = FontWeight.Bold) },
        text = {
            Text("Are you sure you want to restore all settings to their default values?")
        },
        confirmButton = {
            TextButton(onClick = onConfirmReset) {
                Text("Reset", color = MaterialTheme.colorScheme.error)
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
fun CapitalsIndicationDialog(
    visible: Boolean,
    currentMode: Int,
    modes: List<String>,
    onModeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Capitals indication", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (currentMode == index),
                                onClick = { onModeSelected(index) },
                                role = Role.RadioButton
                            )
                            .semantics(mergeDescendants = true) {}
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentMode == index),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

