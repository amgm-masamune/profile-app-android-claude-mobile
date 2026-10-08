package com.example.businesscard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * アプリ全体のテーマ。Soft Glass のトークンを配り、Material3 の ColorScheme にも対応づける。
 * Material3 の標準部品(ダイアログ・文字色など)がトークンと食い違わないようにするのが目的。
 */
@Composable
fun BusinessCardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val glass = if (darkTheme) DarkSoftGlassColors else LightSoftGlassColors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = glass.accentWarm,
            onPrimary = glass.onAccent,
            secondary = glass.accentCool,
            background = glass.backgroundBottom,
            onBackground = glass.ink,
            surface = glass.backgroundTop,
            onSurface = glass.ink,
            onSurfaceVariant = glass.inkMuted,
            error = glass.danger,
            outline = glass.glassBorder,
        )
    } else {
        lightColorScheme(
            primary = glass.accentWarm,
            onPrimary = glass.onAccent,
            secondary = glass.accentCool,
            background = glass.backgroundBottom,
            onBackground = glass.ink,
            surface = glass.backgroundTop,
            onSurface = glass.ink,
            onSurfaceVariant = glass.inkMuted,
            error = glass.danger,
            outline = Color(0xFFB8C1D1),
        )
    }

    CompositionLocalProvider(LocalSoftGlassColors provides glass) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SoftGlassTypography,
            shapes = SoftGlassMaterialShapes,
            content = content,
        )
    }
}
