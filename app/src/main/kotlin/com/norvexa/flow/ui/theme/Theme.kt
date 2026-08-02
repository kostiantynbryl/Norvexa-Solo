package com.norvexa.flow.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF345E52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F0DD),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF4C635B),
    tertiary = Color(0xFF3F6375),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFF7FAF7),
    surface = Color(0xFFF7FAF7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD4C1),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF175044),
    onPrimaryContainer = Color(0xFFB7F0DD),
    secondary = Color(0xFFB3CCC2),
    tertiary = Color(0xFFA7CDDF),
    background = Color(0xFF0F1513),
    surface = Color(0xFF0F1513),
)

@Composable
fun NorvexaFlowTheme(
    themeMode: String = "SYSTEM",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        activity?.let { WindowCompat.getInsetsController(it.window, view).isAppearanceLightStatusBars = !dark }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
