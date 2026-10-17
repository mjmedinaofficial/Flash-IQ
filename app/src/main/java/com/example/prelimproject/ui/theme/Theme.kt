package com.example.prelimproject.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = WebPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = WebCardHoverDark,
    onPrimaryContainer = WebTextPrimaryDark,
    secondary = WebTextSecondaryDark,
    onSecondary = WebTextPrimaryDark,
    tertiary = WebPrimaryTextDark,
    background = WebBgDark,
    onBackground = WebTextPrimaryDark,
    surface = WebCardDark,
    onSurface = WebTextPrimaryDark,
    surfaceVariant = WebCardHoverDark,
    onSurfaceVariant = WebTextSecondaryDark,
    outline = WebBorderDark,
    error = WebDangerDark,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = WebPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEFF6FF),
    onPrimaryContainer = WebPrimaryTextLight,
    secondary = WebTextSecondaryLight,
    onSecondary = Color.White,
    tertiary = WebPrimarySolidLight,
    background = WebBgLight,
    onBackground = WebTextPrimaryLight,
    surface = WebCardLight,
    onSurface = WebTextPrimaryLight,
    surfaceVariant = WebSurfaceLight,
    onSurfaceVariant = WebTextSecondaryLight,
    outline = WebBorderLight,
    error = WebDangerLight,
    onError = Color.White
)

@Composable
fun PrelimProjectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}
