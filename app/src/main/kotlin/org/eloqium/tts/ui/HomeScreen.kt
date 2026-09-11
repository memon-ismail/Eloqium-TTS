package org.eloqium.tts.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.eloqium.tts.R

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 1. Central Logo - concise, single accessible element
            Image(
                painter = painterResource(id = R.drawable.eloqium_logo),
                contentDescription = stringResource(R.string.logo_content_description),
                modifier = Modifier
                    .size(160.dp)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Welcome to Eloqium Heading - exists only once
            Text(
                text = stringResource(R.string.welcome_heading),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() }
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 3. Eloqium Settings Button
            Button(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics(mergeDescendants = true) {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.btn_eloqium_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. System TTS Settings Button - opens Android's text-to-speech settings
            OutlinedButton(
                onClick = { openSystemTtsSettings(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics(mergeDescendants = true) {}
            ) {
                Text(
                    text = stringResource(R.string.btn_system_tts_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. About Button
            OutlinedButton(
                onClick = onNavigateToAbout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics(mergeDescendants = true) {}
            ) {
                Text(
                    text = stringResource(R.string.btn_about),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun openSystemTtsSettings(context: android.content.Context) {
    val intent = android.content.Intent("com.android.settings.TTS_SETTINGS").apply {
        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val accessibilityIntent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(accessibilityIntent)
        } catch (e2: Exception) {
            try {
                val generalIntent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(generalIntent)
            } catch (ignored: Exception) {
            }
        }
    }
}
