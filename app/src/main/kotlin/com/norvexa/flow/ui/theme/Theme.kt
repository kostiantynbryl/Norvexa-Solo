package com.norvexa.flow.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F2FF),
    onPrimaryContainer = Color(0xFF00498F),
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E5EA),
    onSecondaryContainer = Color(0xFF3A3A3C),
    tertiary = Color(0xFF34C759),
    onTertiary = Color.White,
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0xFFFFE9E7),
    onErrorContainer = Color(0xFF8A130D),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF6D6D72),
    outline = Color(0xFFC7C7CC),
    outlineVariant = Color(0xFFE5E5EA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A355F),
    onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = Color(0xFF98989D),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2C2C2E),
    onSecondaryContainer = Color(0xFFE5E5EA),
    tertiary = Color(0xFF30D158),
    onTertiary = Color.Black,
    error = Color(0xFFFF453A),
    onError = Color.Black,
    errorContainer = Color(0xFF571A17),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2),
    outline = Color(0xFF48484A),
    outlineVariant = Color(0xFF38383A),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
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
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        activity?.let {
            val controller = WindowCompat.getInsetsController(it.window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
