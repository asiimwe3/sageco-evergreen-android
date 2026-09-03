package com.sagecoevergreen.app.ui.theme

import androidx.compose.ui.graphics.Color
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = SagecoGreen,
    onPrimary = White,
    primaryContainer = SagecoGreenLight,
    onPrimaryContainer = White,
    secondary = Gold,
    onSecondary = Black,
    background = OffWhite,
    onBackground = Gray800,
    surface = White,
    onSurface = Gray800,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray600,
)

private val DarkColors = darkColorScheme(
    primary = SagecoGreenBright,
    onPrimary = Black,
    primaryContainer = SagecoGreen,
    onPrimaryContainer = White,
    secondary = Gold,
    onSecondary = Black,
    background = Color(0xFF0F172A),
    onBackground = White,
    surface = Color(0xFF1E293B),
    onSurface = White,
)

@Composable
fun SagecoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SagecoGreen.toArgb()
            window.navigationBarColor = SagecoGreen.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
