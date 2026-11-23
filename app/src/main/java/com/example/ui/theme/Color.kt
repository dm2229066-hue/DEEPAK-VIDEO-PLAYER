package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Dark Obsidian & Charcoal Surface Tokens
val ObsidianBlack = Color(0xFF09090B)
val CharcoalSurface = Color(0xFF131317)
val CharcoalContainer = Color(0xFF1B1B21)
val CharcoalElevated = Color(0xFF24242D)
val CharcoalBorder = Color(0xFF2E2E3A)

val TextPrimaryDark = Color(0xFFF5F5F7)
val TextSecondaryDark = Color(0xFFA1A1B0)

// Core Light Surface Tokens
val LightBackground = Color(0xFFF5F5F7)
val LightSurface = Color(0xFFFFFFFF)
val LightContainer = Color(0xFFEBEBEF)
val LightElevated = Color(0xFFE1E1E8)
val LightBorder = Color(0xFFD0D0DC)

val TextPrimaryLight = Color(0xFF121216)
val TextSecondaryLight = Color(0xFF5A5A68)

// Brand & Semantic Tokens
val CinemaGold = Color(0xFFFFD600)
val CinemaGoldDeep = Color(0xFFC7A500)
val AmberFlare = Color(0xFFFFAB00)
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
    GOLD("gold", "Cinema Gold", CinemaGold, Color(0xFF121214), false),
    AMBER("amber", "Amber Flare", AmberFlare, Color(0xFF121214), false),
    LIME("lime", "Electric Lime", ElectricLime, Color(0xFF121214), false),
    CRIMSON("crimson", "Crimson Laser", CrimsonLaser, Color(0xFFFFFFFF), true),
    CYAN("cyan", "Cyber Cyan", CyberCyan, Color(0xFF091216), true);

    companion object {
        fun fromId(id: String): AccentColorOption =
            entries.firstOrNull { it.id == id } ?: GOLD
    }
}
