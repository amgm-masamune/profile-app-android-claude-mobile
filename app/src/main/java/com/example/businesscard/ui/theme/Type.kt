package com.example.businesscard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

/**
 * 文字トークン。書体は端末標準(日本語はシステムフォント)のまま、太さだけで階層を作る。
 * 見出しはBold、ボタン・ラベルはSemiBold、本文はRegular。
 */
private val base = Typography()

val SoftGlassTypography = base.copy(
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)
