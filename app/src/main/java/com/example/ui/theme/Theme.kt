package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = CyanAccent,
  onPrimary = Color(0xFF00363F),
  primaryContainer = Color(0xFF004D59),
  onPrimaryContainer = Color(0xFFB5F4FF),
  secondary = Color(0xFF7986CB),
  onSecondary = Color.White,
  tertiary = GreenBattery,
  onTertiary = Color(0xFF003915),
  background = AudioDarkBackground,
  onBackground = TextPrimaryDark,
  surface = AudioDarkSurface,
  onSurface = TextPrimaryDark,
  surfaceVariant = AudioDarkSurfaceVariant,
  onSurfaceVariant = TextSecondaryDark,
  outline = AudioDarkSurfaceBorder,
  outlineVariant = Color(0xFF21262D),
  error = RedBattery,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF00687A),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFA8EFFF),
  onPrimaryContainer = Color(0xFF001F26),
  secondary = Color(0xFF4B607C),
  onSecondary = Color.White,
  tertiary = Color(0xFF006D37),
  background = Color(0xFFF8F9FA),
  onBackground = Color(0xFF191C1E),
  surface = Color.White,
  onSurface = Color(0xFF191C1E),
  surfaceVariant = Color(0xFFE1E2E5),
  onSurfaceVariant = Color(0xFF444749),
  outline = Color(0xFFD0D3D6),
  error = Color(0xFFBA1A1A)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Default to clean modern white theme matching user reference UI
  dynamicColor: Boolean = false, // Keep consistent audio companion design
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

