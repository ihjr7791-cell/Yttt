package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    secondary = EmeraldDarkSecondary,
    tertiary = EmeraldDarkTertiary,
    background = CosmicSlateBackground,
    surface = CosmicSlateSurface,
    onPrimary = OnCosmicSlateText,
    onSecondary = OnCosmicSlateText,
    onTertiary = CosmicSlateBackground,
    onBackground = OnCosmicSlateText,
    onSurface = OnCosmicSlateText,
    error = CrimsonRoseDanger,
    onError = OnCosmicSlateText,
    surfaceVariant = CosmicSlateSurface.copy(alpha = 0.7f),
    onSurfaceVariant = OnCosmicSlateText
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldLightPrimary,
    secondary = EmeraldLightSecondary,
    tertiary = EmeraldLightTertiary,
    background = CosmicLightBackground,
    surface = CosmicLightSurface,
    onPrimary = OnLightText,
    onSecondary = OnLightText,
    onTertiary = CosmicLightBackground,
    onBackground = OnLightText,
    onSurface = OnLightText,
    error = CrimsonRoseDanger,
    onError = OnLightText,
    surfaceVariant = CosmicLightSurface.copy(alpha = 0.95f),
    onSurfaceVariant = OnLightText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We override dynamic colors to ensure a cohesive luxury Emerald Teal POS experience.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
