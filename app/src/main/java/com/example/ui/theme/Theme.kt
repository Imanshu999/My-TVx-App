package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TvDarkColorScheme = darkColorScheme(
  primary = TvRed,
  onPrimary = Color.White,
  primaryContainer = TvRedDark,
  onPrimaryContainer = Color.White,
  secondary = TvNeonCyan,
  onSecondary = Color.Black,
  secondaryContainer = Color(0xFF004D5A),
  onSecondaryContainer = TvNeonCyan,
  tertiary = TvAccentGlow,
  onTertiary = Color.White,
  background = DarkBackground,
  onBackground = TextPrimary,
  surface = DarkSurface,
  onSurface = TextPrimary,
  surfaceVariant = DarkSurfaceElevated,
  onSurfaceVariant = TextSecondary,
  outline = DarkSurfaceBorder,
  outlineVariant = Color(0xFF191C26)
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = TvDarkColorScheme,
    typography = Typography,
    content = content
  )
}

