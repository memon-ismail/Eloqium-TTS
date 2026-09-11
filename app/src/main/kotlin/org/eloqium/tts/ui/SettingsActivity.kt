package org.eloqium.tts.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.eloqium.tts.service.Settings
import org.eloqium.tts.ui.theme.EloqiumTheme

class SettingsActivity : ComponentActivity() {

    private lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)

        setContent {
            EloqiumTheme {
                SettingsScreen(
                    settings = settings,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
