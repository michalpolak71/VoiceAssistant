package com.voiceassistant.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Kolory ────────────────────────────────────────────────────────────────────
val Primary = Color(0xFF6750A4)
val PrimaryDark = Color(0xFF9A82DB)
val Secondary = Color(0xFF03DAC6)
val Background = Color(0xFF121212)
val Surface = Color(0xFF1E1E1E)
val SurfaceVariant = Color(0xFF2C2C2C)
val OnBackground = Color(0xFFE0E0E0)
val OnSurface = Color(0xFFCCCCCC)

val MedicineColor = Color(0xFFFF6B6B)
val MeetingColor = Color(0xFF4ECDC4)
val TaskColor = Color(0xFFFFE66D)
val GeneralColor = Color(0xFF95E1D3)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = OnBackground,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant
)

@Composable
fun VoiceAssistantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
