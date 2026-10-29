package com.example.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SystemHudColorScheme = darkColorScheme(
    primary = SystemManaCyan,
    onPrimary = AbyssalBackground,
    primaryContainer = SystemWindowSurfaceElevated,
    onPrimaryContainer = SystemManaCyan,
    secondary = MonarchShadowPurple,
    onSecondary = HudTextPrimary,
    secondaryContainer = MonarchDeepViolet,
    onSecondaryContainer = HudTextPrimary,
    tertiary = SovereignRewardGold,
    onTertiary = AbyssalBackground,
    error = PenaltyDangerRed,
    onError = HudTextPrimary,
    background = AbyssalBackground,
    onBackground = HudTextPrimary,
    surface = SystemWindowSurface,
    onSurface = HudTextPrimary,
    surfaceVariant = SystemWindowSurfaceElevated,
    onSurfaceVariant = HudTextSecondary,
    outline = RoyalSystemBlue
)

val SystemHudShapes = Shapes(
    extraSmall = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
    small = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
    medium = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
    large = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp),
    extraLarge = CutCornerShape(topStart = 20.dp, bottomEnd = 20.dp)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SystemHudColorScheme,
        typography = Typography,
        shapes = SystemHudShapes,
        content = content
    )
}
