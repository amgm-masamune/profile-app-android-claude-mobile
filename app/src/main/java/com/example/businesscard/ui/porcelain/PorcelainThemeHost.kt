package com.example.businesscard.ui.porcelain

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.businesscard.domain.model.ThemeStyle
import com.example.businesscard.ui.designsystem.LocalThemeStyle

/**
 * Porcelain のテーマ(単独で使うとき。プレビューや画面のテスト)。
 * アプリ本体は見た目を切り替えるので、BusinessCardAppTheme が同じ値を配る。
 */
@Composable
fun PorcelainTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPorcelainColors provides DefaultPorcelainColors,
        LocalThemeStyle provides ThemeStyle.PORCELAIN,
    ) {
        MaterialTheme(
            colorScheme = porcelainColorScheme(DefaultPorcelainColors),
            typography = PorcelainTypography,
            shapes = PorcelainMaterialShapes,
            content = content,
        )
    }
}

/**
 * Material3 の ColorScheme への対応づけ(Material3 の標準部品が Porcelain の色と食い違わないようにするため)。
 * 明るい板に濃い文字の配色なので、lightColorScheme を土台にする。
 */
fun porcelainColorScheme(c: PorcelainColors): ColorScheme = lightColorScheme(
    primary = c.darkTop,
    onPrimary = c.onDark,
    secondary = c.inkMuted,
    background = c.panelTop,
    onBackground = c.ink,
    surface = c.lightTop,
    onSurface = c.ink,
    onSurfaceVariant = c.inkMuted,
    surfaceContainer = c.lightTop,
    error = c.danger,
    outline = c.inkFaint,
)

val PorcelainTypography: Typography = Typography().copy(
    headlineMedium = PorcelainType.title,
    titleLarge = PorcelainType.dialogTitle,
    titleMedium = PorcelainType.cardCompany,
    bodyLarge = PorcelainType.field,
    bodyMedium = PorcelainType.body,
    labelLarge = PorcelainType.button,
    labelMedium = PorcelainType.label,
)

val PorcelainMaterialShapes = Shapes(
    extraSmall = PorcelainShapes.pill,
    small = PorcelainShapes.pill,
    medium = PorcelainShapes.card,
    large = PorcelainShapes.card,
    extraLarge = PorcelainShapes.dialog,
)
