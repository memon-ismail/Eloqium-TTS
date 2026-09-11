package org.eloqium.tts.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.eloqium.tts.service.Settings
import org.eloqium.tts.ui.theme.EloqiumTheme

enum class Screen {
    HOME,
    SETTINGS,
    ABOUT
}

class MainActivity : ComponentActivity() {

    private lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)

        val initialScreen = if (intent?.action == "android.speech.tts.engine.CONFIGURE_ENGINE") {
            Screen.SETTINGS
        } else {
            Screen.HOME
        }

        setContent {
            EloqiumTheme {
                EloqiumApp(
                    settings = settings,
                    initialScreen = initialScreen,
                    onFinish = { finish() }
                )
            }
        }
    }
}

@Composable
fun EloqiumApp(
    settings: Settings,
    initialScreen: Screen = Screen.HOME,
    onFinish: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(initialScreen) }

    BackHandler(enabled = true) {
        if (currentScreen != Screen.HOME) {
            currentScreen = Screen.HOME
        } else {
            onFinish()
        }
    }

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                onNavigateToAbout = { currentScreen = Screen.ABOUT }
            )
        }
        Screen.SETTINGS -> {
            SettingsScreen(
                settings = settings,
                onNavigateBack = { currentScreen = Screen.HOME }
            )
        }
        Screen.ABOUT -> {
            AboutScreen(
                onNavigateBack = { currentScreen = Screen.HOME }
            )
        }
    }
}
