package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SagePrimaryContainer, // Switch representation for readability in dark mode
    onPrimary = SageOnPrimaryContainer,
    primaryContainer = SagePrimary,
    onPrimaryContainer = SagePrimaryContainer,
    secondary = SageSecondaryContainer,
    onSecondary = SageOnSecondaryContainer,
    tertiary = SageTertiaryContainer,
    onTertiary = SageOnTertiaryContainer,
    background = SageTertiary,
    surface = Color(0xFF232520),
    onBackground = SageBackground,
    onSurface = SageBackground,
    surfaceVariant = SageSecondary,
    onSurfaceVariant = SageSurfaceVariant,
    outline = SageOutlineVariant,
    outlineVariant = SageOutline
)

private val LightColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = SageOnPrimary,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = SageOnPrimaryContainer,
    secondary = SageSecondary,
    onSecondary = SageOnSecondary,
    secondaryContainer = SageSecondaryContainer,
    onSecondaryContainer = SageOnSecondaryContainer,
    tertiary = SageTertiary,
    onTertiary = SageOnTertiary,
    tertiaryContainer = SageTertiaryContainer,
    onTertiaryContainer = SageOnTertiaryContainer,
    background = SageBackground,
    surface = SageSurface,
    onBackground = SageOnBackground,
    onSurface = SageOnSurface,
    surfaceVariant = SageSurfaceVariant,
    onSurfaceVariant = SageOnSurfaceVariant,
    outline = SageOutline,
    outlineVariant = SageOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to strictly honor our beautiful custom theme!
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
