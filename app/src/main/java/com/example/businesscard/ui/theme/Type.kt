package com.example.businesscard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 文字トークン。見本の文字はすべて白で、太さだけで階層を作っている。
 *  - 主ボタン(Launch / Premium plan): Bold
 *  - 副ボタン・入力値・タブ(Secondary / Tabs): Regular
 *  - 部品の上のラベル(Launch / Icon button ...): Medium の小さめ
 * 白い文字は明るいガラスの上で薄れやすいので、ごく弱い影を全ての文字に付ける。
 */
private val InkShadow = Shadow(
    color = Color(0x38000000),
    offset = Offset(0f, 1.5f),
    blurRadius = 5f,
)

object SoftGlassType {
    /** 画面タイトル */
    val title = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, shadow = InkShadow)

    /** 主ボタン(Launch / Premium plan) */
    val buttonStrong = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, shadow = InkShadow)

    /** 副ボタン・タブ・セグメント(Secondary / Tabs) */
    val button = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal, shadow = InkShadow)

    /** 入力欄の値・プレースホルダ・ドロップダウン */
    val field = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal, shadow = InkShadow)

    /** 部品の上に置くラベル */
    val label = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, shadow = InkShadow)

    /** 本文・補助テキスト */
    val body = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, shadow = InkShadow)

    /** エラーなど小さな注記 */
    val caption = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, shadow = InkShadow)

    /** 名刺の氏名 */
    val cardName = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, shadow = InkShadow)

    /** 名刺の会社名 */
    val cardCompany = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, shadow = InkShadow)
}

/** Material3 の標準部品(メニュー項目など)向けの対応づけ。 */
private val base = Typography()

val SoftGlassTypography = base.copy(
    headlineMedium = SoftGlassType.title,
    titleLarge = SoftGlassType.buttonStrong,
    titleMedium = SoftGlassType.cardCompany,
    bodyLarge = SoftGlassType.field,
    bodyMedium = SoftGlassType.body,
    labelLarge = SoftGlassType.button,
    labelMedium = SoftGlassType.label,
)
