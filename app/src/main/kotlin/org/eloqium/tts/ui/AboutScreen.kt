package org.eloqium.tts.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.eloqium.tts.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.btn_about),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate back"
                        }
                    ) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            AboutHeaderSection()

            Spacer(modifier = Modifier.height(20.dp))

            AboutProjectSection()

            Spacer(modifier = Modifier.height(16.dp))

            AboutEngineSection()

            Spacer(modifier = Modifier.height(16.dp))

            AboutPrivacySection()

            Spacer(modifier = Modifier.height(16.dp))

            AboutLicensingSection()

            Spacer(modifier = Modifier.height(16.dp))

            AboutLinksSection(
                onOpenGitHub = { AboutNavigation.openGitHub(context) },
                onOpenCommunity = { AboutNavigation.openTelegram(context) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Notice: Eloqium is an independent open-source project and is not affiliated with, sponsored by, or endorsed by IBM, Nuance Communications, Cerence, Google, or their respective affiliates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AboutHeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(id = R.drawable.eloqium_logo),
            contentDescription = stringResource(R.string.logo_content_description),
            modifier = Modifier.size(96.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "High-performance text-to-speech engine for Android",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AboutProjectSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Project Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(label = "Application", value = "Eloqium")
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Creator / Maintainer", value = "Ismail Memon")
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(
                label = "Special Thanks To",
                value = "1. Chandu Rathod\n2. Yashraj Shinde"
            )
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Engine Identifier", value = "Eloqium TTS (org.eloqium.tts)")
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Version", value = "0.1.3")
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "License", value = "Apache License 2.0 (Open Source)")
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Target Architecture", value = "ARM64-v8a / ARMv7a / x86_64")
        }
    }
}

@Composable
private fun AboutEngineSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Engine Architecture & Lineage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Eloqium provides real-time speech synthesis by integrating an optimized native build of OpenEVV with modern Android TextToSpeechService architecture. Built specifically for high-rate intelligibility, low audio latency, and robust screen-reader navigation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Features include multi-lingual user dictionaries (text and OpenEVV SPR pronunciation), context-aware abbreviation expansion, full emoji and emoticon vocalization, configurable pause durations, and seamless screen-reader punctuation handling.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutPrivacySection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Privacy & Offline Architecture",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Eloqium operates entirely on-device. The application requires zero network permissions, contains no analytics or tracking libraries, and processes all spoken text locally inside the isolated native speech engine.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AboutLicensingSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Licensing & Attributions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(10.dp))

            InfoRow(label = "Eloqium TTS", value = "Apache License 2.0")
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "OpenEVV Native Engine", value = "MIT License (Stanislaw Przedzinkowski)")
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Android Platform & AOSP", value = "Apache License 2.0")
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Unicode CLDR & Emoji Data", value = "Unicode License (Unicode, Inc.)")
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "EVVDroid", value = "Apache License 2.0 (Architecture Reference)")
        }
    }
}

@Composable
private fun AboutLinksSection(
    onOpenGitHub: () -> Unit,
    onOpenCommunity: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Project & Community",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onOpenGitHub,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics(mergeDescendants = true) {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.btn_github),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenCommunity,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics(mergeDescendants = true) {}
            ) {
                Text(
                    text = stringResource(R.string.btn_telegram),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
