package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DeepakShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DeepakVideoPlayerTheme(
    darkTheme: Boolean = true,
    accentOption: AccentColorOption = AccentColorOption.GOLD,
    content: @Composable () -> Unit
) {
    val accent = accentOption.color
    val onAccent = accentOption.onAccent

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.16f),
            onPrimaryContainer = accent,
            secondary = AmberFlare,
            onSecondary = ObsidianBlack,
            secondaryContainer = CharcoalElevated,
            onSecondaryContainer = TextPrimaryDark,
            tertiary = SemanticSuccess,
            background = ObsidianBlack,
            onBackground = TextPrimaryDark,
            surface = CharcoalSurface,
            onSurface = TextPrimaryDark,
            surfaceVariant = CharcoalContainer,
            onSurfaceVariant = TextSecondaryDark,
            surfaceContainerLowest = ObsidianBlack,
            surfaceContainerLow = CharcoalSurface,
            surfaceContainer = CharcoalContainer,
            surfaceContainerHigh = CharcoalElevated,
            outline = CharcoalBorder,
            outlineVariant = CharcoalBorder.copy(alpha = 0.55f),
            error = SemanticError,
            onError = TextPrimaryDark
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.22f),
            onPrimaryContainer = TextPrimaryLight,
            secondary = CinemaGoldDeep,
            onSecondary = LightSurface,
            secondaryContainer = LightContainer,
            onSecondaryContainer = TextPrimaryLight,
            tertiary = SemanticSuccess,
            background = LightBackground,
            onBackground = TextPrimaryLight,
            surface = LightSurface,
            onSurface = TextPrimaryLight,
            surfaceVariant = LightContainer,
            onSurfaceVariant = TextSecondaryLight,
            surfaceContainerLowest = LightSurface,
            surfaceContainerLow = LightBackground,
            surfaceContainer = LightContainer,
            surfaceContainerHigh = LightElevated,
            outline = LightBorder,
            outlineVariant = LightBorder.copy(alpha = 0.7f),
            error = SemanticError,
            onError = LightSurface
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DeepakTypography,
        shapes = DeepakShapes,
        content = content
    )
}
