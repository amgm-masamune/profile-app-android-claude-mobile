package com.example.businesscard.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.businesscard.data.model.BusinessCard
import com.example.businesscard.ui.theme.SoftGlassShapes
import com.example.businesscard.ui.theme.SoftGlassTheme

/**
 * 名刺の見た目(レイアウト固定)。一覧と表示画面で共通に使う。
 * ガラス面の中に、暖色・寒色の淡い発光が角から滲む。
 *
 * @param onClick 指定すると面全体がボタンになる(波紋は角丸の内側に収まる)
 */
@Composable
fun BusinessCardView(
    card: BusinessCard,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    // 将来、名刺ごとの背景色・文字色に差し替えるための引数(null = ガラス面)
    backgroundColor: Color? = null,
    textColor: Color = SoftGlassTheme.colors.ink,
) {
    val c = SoftGlassTheme.colors
    val shape = SoftGlassShapes.card
    val mutedColor = textColor.copy(alpha = 0.72f)
    val warmAlpha = if (c.isDark) 0.35f else 0.60f
    val coolAlpha = if (c.isDark) 0.30f else 0.40f

    Column(
        modifier = modifier
            .aspectRatio(CARD_ASPECT_RATIO)
            .glassSurface(shape = shape, fill = backgroundColor ?: c.glass)
            .then(
                if (onClick != null) {
                    Modifier.clip(shape).clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(c.accentWarmSoft.copy(alpha = warmAlpha), Color.Transparent),
                        center = Offset(size.width * 0.94f, size.height * 0.10f),
                        radius = size.width * 0.5f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(c.accentCool.copy(alpha = coolAlpha), Color.Transparent),
                        center = Offset(size.width * 0.04f, size.height * 0.98f),
                        radius = size.width * 0.45f,
                    ),
                )
            }
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            if (card.company.isNotEmpty()) {
                Text(
                    text = card.company,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (card.title.isNotEmpty()) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = card.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column {
            if (card.phone.isNotEmpty()) {
                Text(
                    text = card.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedColor,
                    maxLines = 1,
                )
            }
            if (card.email.isNotEmpty()) {
                Text(
                    text = card.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 日本の名刺の標準サイズ 91mm x 55mm */
private const val CARD_ASPECT_RATIO = 91f / 55f
