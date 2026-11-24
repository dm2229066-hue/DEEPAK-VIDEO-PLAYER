package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Deep OLED Obsidian & Machined Glass Surface Tokens
val ObsidianBlack = Color(0xFF060608)
val CharcoalSurface = Color(0xFF101015)
val CharcoalContainer = Color(0xFF17171F)
val CharcoalElevated = Color(0xFF21212C)
val CharcoalBorder = Color(0xFF2C2C3A)
val GlassHighlight = Color(0x1FFFFFFF)

val TextPrimaryDark = Color(0xFFF7F7FA)
val TextSecondaryDark = Color(0xFF9C9CAE)

// Crisp Alabaster Light Surface Tokens
val LightBackground = Color(0xFFF3F4F8)
val LightSurface = Color(0xFFFFFFFF)
val LightContainer = Color(0xFFE9EAF0)
val LightElevated = Color(0xFFDEDFE8)
val LightBorder = Color(0xFFCFD1DE)

val TextPrimaryLight = Color(0xFF101016)
val TextSecondaryLight = Color(0xFF5A5A6C)

// Signature Cinema Accents
val CinemaGold = Color(0xFFFFD426)
val CinemaGoldDeep = Color(0xFFB89200)
val AmberFlare = Color(0xFFFF9F0A)
val ElectricLime = Color(0xFFC6FF00)
val CrimsonLaser = Color(0xFFFF3D57)
val CyberCyan = Color(0xFF00E5FF)

val SemanticSuccess = Color(0xFF22C55E)
val SemanticWarning = Color(0xFFF59E0B)
val SemanticError = Color(0xFFEF4444)

enum class AccentColorOption(
    val id: String,
    val label: String,
    val color: Color,
    val onAccent: Color,
    val requiresRewardUnlock: Boolean
) {
    GOLD("gold", "Cinema Gold", CinemaGold, Color(0xFF101014), false),
    AMBER("amber", "Amber Flare", AmberFlare, Color(0xFF101014), false),
    LIME("lime", "Electric Lime", ElectricLime, Color(0xFF101014), false),
    CRIMSON("crimson", "Crimson Laser", CrimsonLaser, Color(0xFFFFFFFF), true),
    CYAN("cyan", "Cyber Cyan", CyberCyan, Color(0xFF08141A), true);

    companion object {
        fun fromId(id: String): AccentColorOption =
            entries.firstOrNull { it.id == id } ?: GOLD
    }
}
