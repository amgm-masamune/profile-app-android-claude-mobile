package com.example.businesscard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * デザインシステム「Soft Glass」の色トークン(グレージュ版)。
 *
 * 見本(グレージュの壁に、すりガラスの部品が浮いている写真風のUIキット)から色を拾っている。
 *  - 壁: 左上から斜めに光が差し、左下が陰になったグレージュ
 *  - ガラス: 白の薄い膜(18%)。押す・選ぶと白が濃くなる
 *  - 光: 部品の下端が白く光り、壁にも光が漏れる
 *  - 影: 右下に落ちる茶色がかった影
 *  - 文字: すべて白
 *
 * 見本は昼の一枚なので、ライト/ダークの切り替えは持たない(端末設定に関係なく同じ見た目)。
 * 画面に直接 Color(0x...) を書かず、必ずこのトークン経由で使う。
 */
@Immutable
data class SoftGlassColors(
    // 壁(画面背景)
    val wallTop: Color,
    val wallBottom: Color,
    val wallLight: Color,
    val wallShade: Color,
    // ガラス面
    val glass: Color,
    val glassPressed: Color,
    val glassSelected: Color,
    val glassTile: Color,
    val glassSheen: Color,
    val glassBorder: Color,
    val focusBorder: Color,
    val dialog: Color,
    // 光と影
    val glow: Color,
    val castShadow: Color,
    val thumbTop: Color,
    val thumbBottom: Color,
    // 文字
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val danger: Color,
)

val GreigeSoftGlassColors = SoftGlassColors(
    wallTop = Color(0xFFAA9B8E),
    wallBottom = Color(0xFF968677),
    wallLight = Color(0xFFE2D7CB),
    wallShade = Color(0xFF6E6054),
    glass = Color(0x2EFFFFFF),
    glassPressed = Color(0x4DFFFFFF),
    glassSelected = Color(0x52FFFFFF),
    glassTile = Color(0x73FFFFFF),
    glassSheen = Color(0x40FFFFFF),
    glassBorder = Color(0x8CFFFFFF),
    focusBorder = Color(0xF2FFFFFF),
    dialog = Color(0xFFAC9D90),
    glow = Color(0xFFFFFCF6),
    castShadow = Color(0x55281C12),
    thumbTop = Color(0xFFF7F4F0),
    thumbBottom = Color(0xFFDAD5CF),
    ink = Color(0xFFFFFFFF),
    inkMuted = Color(0xC7FFFFFF),
    inkFaint = Color(0x99FFFFFF),
    danger = Color(0xFFFFC7BD),
)

val LocalSoftGlassColors = staticCompositionLocalOf { GreigeSoftGlassColors }

/** `SoftGlassTheme.colors.ink` のように参照する入口。 */
object SoftGlassTheme {
    val colors: SoftGlassColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSoftGlassColors.current
}
