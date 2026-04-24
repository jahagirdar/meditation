package com.serenity.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────
// Accent colours (6 presets)
// ──────────────────────────────────────────────

object AccentColours {
    val SlateBlue  = Color(0xFF5B7FA6)
    val ForestGreen = Color(0xFF4A7C6F)
    val WarmAmber  = Color(0xFFC48B40)
    val DustyRose  = Color(0xFFA66070)
    val OceanTeal  = Color(0xFF3D8080)
    val Sandstone  = Color(0xFF8C7A60)

    fun fromKey(key: String) = when (key) {
        "slate_blue"   -> SlateBlue
        "forest_green" -> ForestGreen
        "warm_amber"   -> WarmAmber
        "dusty_rose"   -> DustyRose
        "ocean_teal"   -> OceanTeal
        "sandstone"    -> Sandstone
        else           -> SlateBlue
    }

    val all = listOf(
        "slate_blue"   to SlateBlue,
        "forest_green" to ForestGreen,
        "warm_amber"   to WarmAmber,
        "dusty_rose"   to DustyRose,
        "ocean_teal"   to OceanTeal,
        "sandstone"    to Sandstone,
    )
}

// ──────────────────────────────────────────────
// Colour schemes
// ──────────────────────────────────────────────

private fun lightScheme(accent: Color) = lightColorScheme(
    primary         = accent,
    onPrimary       = Color.White,
    primaryContainer = accent.copy(alpha = 0.15f),
    onPrimaryContainer = accent,
    background      = Color(0xFFF8F6F2),
    onBackground    = Color(0xFF1A1A1A),
    surface         = Color(0xFFFFFFFF),
    onSurface       = Color(0xFF1A1A1A),
    surfaceVariant  = Color(0xFFEEEBE4),
    onSurfaceVariant = Color(0xFF49454F),
    outline         = Color(0xFFCCC8C0),
)

private fun darkScheme(accent: Color) = darkColorScheme(
    primary         = accent.copy(alpha = 0.85f),
    onPrimary       = Color.Black,
    primaryContainer = accent.copy(alpha = 0.25f),
    onPrimaryContainer = accent.copy(alpha = 0.85f),
    background      = Color(0xFF1A1C1E),
    onBackground    = Color(0xFFE3E1DC),
    surface         = Color(0xFF22252A),
    onSurface       = Color(0xFFE3E1DC),
    surfaceVariant  = Color(0xFF2E3136),
    onSurfaceVariant = Color(0xFFCBC8C0),
    outline         = Color(0xFF4A4740),
)

private fun amoledScheme(accent: Color) = darkColorScheme(
    primary         = accent,
    onPrimary       = Color.Black,
    primaryContainer = accent.copy(alpha = 0.2f),
    onPrimaryContainer = accent,
    background      = Color(0xFF000000),
    onBackground    = Color(0xFFE3E1DC),
    surface         = Color(0xFF0D0F11),
    onSurface       = Color(0xFFE3E1DC),
    surfaceVariant  = Color(0xFF161A1E),
    onSurfaceVariant = Color(0xFFCBC8C0),
    outline         = Color(0xFF2A2720),
)

// ──────────────────────────────────────────────
// Typography
// ──────────────────────────────────────────────

val SerenityTypography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
)

// ──────────────────────────────────────────────
// Theme composable
// ──────────────────────────────────────────────

@Composable
fun SerenityTheme(
    themeMode: String = "system",
    accentColour: String = "slate_blue",
    content: @Composable () -> Unit,
) {
    val accent = AccentColours.fromKey(accentColour)
    val isDark = when (themeMode) {
        "light"  -> false
        "dark", "amoled" -> true
        else     -> isSystemInDarkTheme()
    }
    val colorScheme = when {
        themeMode == "amoled"  -> amoledScheme(accent)
        isDark                 -> darkScheme(accent)
        else                   -> lightScheme(accent)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SerenityTypography,
        content     = content,
    )
}
