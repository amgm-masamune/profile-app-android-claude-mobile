package com.example.businesscard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * デザインシステム「Soft Glass」の色トークン。
 *
 * 画面に直接 Color(0x...) を書かず、必ずこのトークン経由で使う。
 * Material3 の ColorScheme では表せない「ガラス面」「発光」などを持つため、
 * ColorScheme とは別に CompositionLocal で配る。
 */
@Immutable
data class SoftGlassColors(
    val isDark: Boolean,
    // 背景(縦グラデーション + 発光ブロブ)
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val blobWarm: Color,
    val blobCool: Color,
    // ガラス面
    val glass: Color,
    val glassStrong: Color,
    val glassBorder: Color,
    val dialog: Color,
    val shadow: Color,
    // 文字
    val ink: Color,
    val inkMuted: Color,
    // アクセント
    val accentWarm: Color,
    val accentWarmSoft: Color,
    val accentCool: Color,
    val onAccent: Color,
    val focusRing: Color,
    val danger: Color,
)

val LightSoftGlassColors = SoftGlassColors(
    isDark = false,
    backgroundTop = Color(0xFFF4F6F9),
    backgroundBottom = Color(0xFFDFE5EE),
    blobWarm = Color(0xFFFFB36B),
    blobCool = Color(0xFF8EC5F2),
    glass = Color(0xA8FFFFFF),
    glassStrong = Color(0xD9FFFFFF),
    glassBorder = Color(0xE6FFFFFF),
    dialog = Color(0xF5FFFFFF),
    shadow = Color(0x4D5B6B86),
    ink = Color(0xFF1E2431),
    inkMuted = Color(0xFF576074),
    accentWarm = Color(0xFFFF8A3D),
    accentWarmSoft = Color(0xFFFFC48A),
    accentCool = Color(0xFF4F9BE0),
    onAccent = Color(0xFF2A1706),
    focusRing = Color(0xFFD9640F),
    danger = Color(0xFFC9372F),
)

val DarkSoftGlassColors = SoftGlassColors(
    isDark = true,
    backgroundTop = Color(0xFF151A25),
    backgroundBottom = Color(0xFF0A0D13),
    blobWarm = Color(0xFFFF8A3D),
    blobCool = Color(0xFF3D7BC0),
    glass = Color(0x1FFFFFFF),
    glassStrong = Color(0x33FFFFFF),
    glassBorder = Color(0x38FFFFFF),
    dialog = Color(0xFF1C2230),
    shadow = Color(0x99000000),
    ink = Color(0xFFEEF1F6),
    inkMuted = Color(0xFFA6AFBF),
    accentWarm = Color(0xFFFF8A3D),
    accentWarmSoft = Color(0xFFFFC48A),
    accentCool = Color(0xFF6DB3F0),
    onAccent = Color(0xFF2A1706),
    focusRing = Color(0xFFFFA463),
    danger = Color(0xFFFF7A72),
)

val LocalSoftGlassColors = compositionLocalOf { LightSoftGlassColors }

/** `SoftGlassTheme.colors.ink` のように参照する入口。 */
object SoftGlassTheme {
    val colors: SoftGlassColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSoftGlassColors.current
}
