package com.kstoya.univer.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF241A00),
    secondary = AccentLight,
    background = Bg,
    onBackground = TextMain,
    surface = Surface,
    onSurface = TextMain,
    surfaceVariant = Color(0xFF26262F),
    onSurfaceVariant = Muted,
    error = Color(0xFFEF4444)
)

private val LightScheme = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color.White,
    secondary = Accent,
    background = Color(0xFFF6F6F8),
    onBackground = Color(0xFF111114),
    surface = Color.White,
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFEDEDF1),
    onSurfaceVariant = Color(0xFF6B7280),
    error = Color(0xFFB91C1C)
)

/** Тёмная тема по умолчанию: карточка должна быть контрастной всегда. */
@Composable
fun KtoYaTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val scheme = if (dark) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }
    CompositionLocalProvider(LocalContentColor provides scheme.onBackground) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
