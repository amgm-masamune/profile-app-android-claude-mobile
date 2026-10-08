package com.example.businesscard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * アプリ全体のテーマ。Soft Glass のトークンを配り、Material3 の ColorScheme にも対応づける。
 * Material3 の標準部品(メニュー・ダイアログなど)がトークンと食い違わないようにするのが目的。
 *
 * 文字が白い配色なので、Material3 側は darkColorScheme を土台にする。
 */
@Composable
fun BusinessCardTheme(content: @Composable () -> Unit) {
    val glass = GreigeSoftGlassColors
    val colorScheme = darkColorScheme(
        primary = glass.ink,
        onPrimary = glass.wallBottom,
        secondary = glass.inkMuted,
        background = glass.wallBottom,
        onBackground = glass.ink,
        surface = glass.dialog,
        onSurface = glass.ink,
        onSurfaceVariant = glass.inkMuted,
        surfaceContainer = glass.dialog,
        error = glass.danger,
        outline = glass.glassBorder,
    )

    CompositionLocalProvider(LocalSoftGlassColors provides glass) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SoftGlassTypography,
            shapes = SoftGlassMaterialShapes,
            content = content,
        )
    }
}
