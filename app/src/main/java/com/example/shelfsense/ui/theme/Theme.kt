package com.example.shelfsense.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LocalShelfColors = staticCompositionLocalOf { LightShelfColors }

// screens read brand colours from here, Material components read the scheme built below
object ShelfTheme {
    val colors: ShelfColors
        @Composable
        @ReadOnlyComposable
        get() = LocalShelfColors.current
}

// every role is set on purpose. anything left out falls back to the Material baseline,
// which is purple, and it shows up in date pickers, menus and dialogs
private fun ShelfColors.toScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = tint,
        onPrimaryContainer = primary,
        inversePrimary = if (isDark) LightShelfColors.primary else sage,
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = tint,
        onSecondaryContainer = primary,
        tertiary = info,
        onTertiary = if (isDark) background else Color.White,
        tertiaryContainer = infoBg,
        onTertiaryContainer = info,
        background = background,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = chip,
        onSurfaceVariant = muted,
        surfaceTint = primary,
        inverseSurface = ink,
        inverseOnSurface = background,
        error = urgent,
        onError = if (isDark) background else Color.White,
        errorContainer = urgentBg,
        onErrorContainer = urgent,
        outline = line,
        outlineVariant = line,
        scrim = Color(0x99000000),
        surfaceBright = surface,
        surfaceDim = background,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = if (isDark) chip else background
    )
}

@Composable
fun ShelfSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkShelfColors else LightShelfColors
    val scheme = remember(colors) { colors.toScheme() }
    // dynamic colour stays off so the brand palette looks the same on every device
    CompositionLocalProvider(LocalShelfColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = ShelfTypography, content = content)
    }
}
