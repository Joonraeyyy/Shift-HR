package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    themeName: String = "Sapphire Glass",
    content: @Composable () -> Unit
) {
    val themeColors = LiquidThemeRegistry.getThemeByName(themeName)
    
    val dynamicColorScheme = darkColorScheme(
        primary = themeColors.primaryAccent,
        secondary = themeColors.secondaryAccent,
        tertiary = themeColors.primaryAccent,
        background = themeColors.bgGradientStart,
        surface = themeColors.cardSurface,
        onPrimary = TextDark,
        onSecondary = TextDark,
        onBackground = themeColors.textPrimary,
        onSurface = themeColors.textPrimary,
        surfaceVariant = themeColors.cardSurface,
        onSurfaceVariant = themeColors.textPrimary
    )

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = Typography,
        content = content
    )
}
