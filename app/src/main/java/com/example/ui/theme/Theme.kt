package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DoorblyTealLight,
    onPrimary = DoorblyOnTealContainer,
    primaryContainer = DoorblyTealDark,
    onPrimaryContainer = DoorblyTealContainer,
    secondary = DoorblyIndigoLight,
    onSecondary = Slate900,
    secondaryContainer = Slate800,
    onSecondaryContainer = DoorblyIndigoContainer,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    outline = Slate500
)

private val LightColorScheme = lightColorScheme(
    primary = DoorblyTeal,
    onPrimary = Color.White,
    primaryContainer = DoorblyTealContainer,
    onPrimaryContainer = DoorblyOnTealContainer,
    secondary = DoorblyIndigo,
    onSecondary = Color.White,
    secondaryContainer = DoorblyIndigoContainer,
    onSecondaryContainer = DoorblyOnIndigoContainer,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate300
)

@Composable
fun DoorblyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
