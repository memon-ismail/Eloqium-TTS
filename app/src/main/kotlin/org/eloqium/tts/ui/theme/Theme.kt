package org.eloqium.tts.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RoyalCobalt = Color(0xFF2030E8)
val DarkNavy = Color(0xFF182070)
val CobaltAccent = Color(0xFF2830F0)

val LightColors = lightColorScheme(
    primary = RoyalCobalt,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E2FF),
    onPrimaryContainer = Color(0xFF001258),
    secondary = DarkNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE1FF),
    onSecondaryContainer = Color(0xFF04154B),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFE2E2EC),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF757680)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFBEC2FF),
    onPrimary = Color(0xFF001BB3),
    primaryContainer = RoyalCobalt,
    onPrimaryContainer = Color(0xFFE0E2FF),
    secondary = Color(0xFFB8C4FF),
    onSecondary = DarkNavy,
    secondaryContainer = Color(0xFF283280),
    onSecondaryContainer = Color(0xFFDCE1FF),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE3E2E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE3E2E9),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC5C6D0),
    outline = Color(0xFF8F909A)
)

@Composable
fun EloqiumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
