package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme

val AppTextColor: Color
    @Composable
    get() = MaterialTheme.colorScheme.onBackground

val AppTextSecondaryColor: Color
    @Composable
    get() = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

// ClauseOS Compliance SaaS Premium Palette
val CyberBg = Color(0xFF090D16)      // Sleek Dark Charcoal/Navy Slate
val CyberSurface = Color(0xFF111723) // High-end Slate Card Surface
val CyberSurfaceVariant = Color(0xFF1A2234) // Lighter Slate Accent Surface
val CyberPrimary = Color(0xFF00D19C) // ClauseOS Mint Green (Compliance & Status Accent)
val CyberSecondary = Color(0xFF6366F1) // ClauseOS Electric Royal Indigo Accent
val CyberTertiary = Color(0xFF38BDF8)  // ClauseOS Tech Cyan Accent

// Status Colors
val CyberSuccess = Color(0xFF10B981) // Emerald Green
val CyberWarning = Color(0xFFF59E0B) // Amber Gold
val CyberAlert = Color(0xFFF43F5E)   // Neon Coral/Rose
val CyberGray = Color(0xFF64748B)    // Cool slate gray

val TextPrimary = Color(0xFFF8FAFC)  // White
val TextSecondary = Color(0xFF94A3B8) // Slate 400
val TextDark = Color(0xFF020617)     // Dark Slate 950

// Apple-style Liquid Glass Theme definitions
data class LiquidThemeColors(
    val name: String,
    val bgGradientStart: Color,
    val bgGradientEnd: Color,
    val bgBubble1: Color,
    val bgBubble2: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val textPrimary: Color = Color(0xFFFFFFFF),
    val textSecondary: Color = Color(0xFFCBD5E1),
    val isLightTheme: Boolean = false
)

object LiquidThemeRegistry {
    val SapphireGlass = LiquidThemeColors(
        name = "Sapphire Glass",
        bgGradientStart = Color(0xFF070B14),
        bgGradientEnd = Color(0xFF0F172A),
        bgBubble1 = Color(0xFF1D4ED8), // Deep Sapphire Blue
        bgBubble2 = Color(0xFF312E81), // Deep Royal Purple
        primaryAccent = Color(0xFF38BDF8), // Neon Light Blue
        secondaryAccent = Color(0xFF6366F1), // Royal Indigo
        cardSurface = Color(0x3D0D1527), // Semi-transparent glass card
        cardBorder = Color(0x2EFFFFFF) // Liquid specular highlight
    )

    val EmeraldGlass = LiquidThemeColors(
        name = "Emerald Glass",
        bgGradientStart = Color(0xFF02120E),
        bgGradientEnd = Color(0xFF081C15),
        bgBubble1 = Color(0xFF064E3B), // Dark Forest Green
        bgBubble2 = Color(0xFF14532D), // Deep Emerald
        primaryAccent = Color(0xFF10B981), // Vibrant Mint/Green
        secondaryAccent = Color(0xFF00D19C), // Bright Teal
        cardSurface = Color(0x3D022C22),
        cardBorder = Color(0x38FFFFFF)
    )

    val MidnightGlass = LiquidThemeColors(
        name = "Midnight Glass",
        bgGradientStart = Color(0xFF000000),
        bgGradientEnd = Color(0xFF0E0E10),
        bgBubble1 = Color(0xFF1F2937), // Soft slate graphite
        bgBubble2 = Color(0xFF111827), // Steel dark gray
        primaryAccent = Color(0xFFF1F5F9), // Metallic Silver
        secondaryAccent = Color(0xFF94A3B8), // Cool Slate
        cardSurface = Color(0x3D111111),
        cardBorder = Color(0x3BFFFFFF)
    )

    val AmethystGlass = LiquidThemeColors(
        name = "Amethyst Glass",
        bgGradientStart = Color(0xFF0E0715),
        bgGradientEnd = Color(0xFF1B0F27),
        bgBubble1 = Color(0xFF4C1D95), // Royal Purple
        bgBubble2 = Color(0xFF581C87), // Deep Plum Amethyst
        primaryAccent = Color(0xFFE879F9), // Luminous Fuchsia
        secondaryAccent = Color(0xFFF43F5E), // Luminous Coral Rose
        cardSurface = Color(0x3D1F1235),
        cardBorder = Color(0x35FFFFFF)
    )

    val SunsetGlass = LiquidThemeColors(
        name = "Sunset Glass",
        bgGradientStart = Color(0xFF150907),
        bgGradientEnd = Color(0xFF22110E),
        bgBubble1 = Color(0xFF78350F), // Warm Terracotta Amber
        bgBubble2 = Color(0xFF7C2D12), // Deep Sienna Copper
        primaryAccent = Color(0xFFFBBF24), // Vibrant Honey Gold
        secondaryAccent = Color(0xFFF97316), // Saturated Orange
        cardSurface = Color(0x3D29120E),
        cardBorder = Color(0x33FFFFFF)
    )

    // LIGHT GLASS Presets matching their dark counterparts elegantly
    val SapphireLightGlass = LiquidThemeColors(
        name = "Sapphire Light",
        bgGradientStart = Color(0xFFF0F4FF),
        bgGradientEnd = Color(0xFFE0E7FF),
        bgBubble1 = Color(0xFF93C5FD), // Soft sky blue bubble
        bgBubble2 = Color(0xFFC7D2FE), // Soft indigo bubble
        primaryAccent = Color(0xFF2563EB), // Clear royal blue
        secondaryAccent = Color(0xFF4F46E5), // Clear indigo
        cardSurface = Color(0x80FFFFFF), // Semi-transparent white glass card (50% opacity)
        cardBorder = Color(0x24000000), // Soft dark border
        textPrimary = Color(0xFF0F172A), // Slate 900
        textSecondary = Color(0xFF475569), // Slate 600
        isLightTheme = true
    )

    val EmeraldLightGlass = LiquidThemeColors(
        name = "Emerald Light",
        bgGradientStart = Color(0xFFF0FDF4),
        bgGradientEnd = Color(0xFFDCFCE7),
        bgBubble1 = Color(0xFF86EFAC), // Soft mint bubble
        bgBubble2 = Color(0xFFA7F3D0), // Soft emerald bubble
        primaryAccent = Color(0xFF059669), // Clear emerald
        secondaryAccent = Color(0xFF0D9488), // Clear teal
        cardSurface = Color(0x80FFFFFF),
        cardBorder = Color(0x24000000),
        textPrimary = Color(0xFF062F21),
        textSecondary = Color(0xFF1F2937),
        isLightTheme = true
    )

    val AlabasterLightGlass = LiquidThemeColors(
        name = "Alabaster Light",
        bgGradientStart = Color(0xFFFAFAFA),
        bgGradientEnd = Color(0xFFF4F4F5),
        bgBubble1 = Color(0xFFE4E4E7), // Soft gray bubble
        bgBubble2 = Color(0xFFD4D4D8), // Soft steel bubble
        primaryAccent = Color(0xFF18181B), // Ink/Dark charcoal
        secondaryAccent = Color(0xFF52525B), // Medium charcoal
        cardSurface = Color(0x80FFFFFF),
        cardBorder = Color(0x24000000),
        textPrimary = Color(0xFF09090B),
        textSecondary = Color(0xFF3F3F46),
        isLightTheme = true
    )

    val AmethystLightGlass = LiquidThemeColors(
        name = "Amethyst Light",
        bgGradientStart = Color(0xFFFAF5FF),
        bgGradientEnd = Color(0xFFF3E8FF),
        bgBubble1 = Color(0xFFD8B4FE), // Soft amethyst bubble
        bgBubble2 = Color(0xFFF3C0F9), // Soft fuchsia bubble
        primaryAccent = Color(0xFF7C3AED), // Clear purple
        secondaryAccent = Color(0xFFD946EF), // Clear fuchsia
        cardSurface = Color(0x80FFFFFF),
        cardBorder = Color(0x24000000),
        textPrimary = Color(0xFF1E1B4B),
        textSecondary = Color(0xFF4338CA),
        isLightTheme = true
    )

    val SunsetLightGlass = LiquidThemeColors(
        name = "Sunset Light",
        bgGradientStart = Color(0xFFFFF7ED),
        bgGradientEnd = Color(0xFFFFEDD5),
        bgBubble1 = Color(0xFFFDBA74), // Soft amber bubble
        bgBubble2 = Color(0xFFFECDD3), // Soft rose bubble
        primaryAccent = Color(0xFFD97706), // Clear warm amber
        secondaryAccent = Color(0xFFEA580C), // Clear warm orange
        cardSurface = Color(0x80FFFFFF),
        cardBorder = Color(0x24000000),
        textPrimary = Color(0xFF451A03),
        textSecondary = Color(0xFF7C2D12),
        isLightTheme = true
    )

    val allThemes = listOf(
        SapphireGlass, EmeraldGlass, MidnightGlass, AmethystGlass, SunsetGlass,
        SapphireLightGlass, EmeraldLightGlass, AlabasterLightGlass, AmethystLightGlass, SunsetLightGlass
    )

    fun getThemeByName(name: String): LiquidThemeColors {
        return allThemes.find { it.name.lowercase() == name.lowercase() } ?: SapphireGlass
    }
}
