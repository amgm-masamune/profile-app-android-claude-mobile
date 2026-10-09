package com.example.businesscard.ui.porcelain

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.ui.glass.rememberGlassFeedback

/**
 * 名刺(レイアウト固定)。見本の明るいボタンと同じ「陶器の板」を、名刺の大きさにしたもの。
 * 大きな板なので高く浮き、影も長い。
 *
 * @param onClick 指定すると面全体がボタンになる。押すと板が沈み込み、離すとばねで浮き上がる
 */
@Composable
fun PorcelainBusinessCard(
    card: BusinessCard,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    // 将来、名刺ごとの文字色に差し替えるための引数
    textColor: Color = PorcelainTheme.colors.ink,
) {
    val c = PorcelainTheme.colors
    val shape = PorcelainShapes.card
    val mutedColor = if (textColor == c.ink) c.inkMuted else textColor.copy(alpha = textColor.alpha * 0.75f)
    val interaction = remember { MutableInteractionSource() }
    val feedback = rememberGlassFeedback()
    val press = if (onClick != null) rememberPorcelainPress(interaction, feedback) else null

    Column(
        modifier = modifier
            .aspectRatio(CARD_ASPECT_RATIO)
            .porcelainSurface(shape = shape, tone = PorcelainTone.Light, elevation = PorcelainElevation.Card, pressed = press)
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(shape)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClick = {
                                feedback.click()
                                onClick()
                            },
                        )
                } else {
                    Modifier
                },
            )
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            if (card.company.isNotEmpty()) {
                Text(text = card.company, style = PorcelainType.cardCompany, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (card.title.isNotEmpty()) {
                Text(text = card.title, style = PorcelainType.body, color = mutedColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(text = card.name, style = PorcelainType.cardName, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Column {
            if (card.phone.isNotEmpty()) {
                Text(text = card.phone, style = PorcelainType.body, color = mutedColor, maxLines = 1)
            }
            if (card.email.isNotEmpty()) {
                Text(text = card.email, style = PorcelainType.body, color = mutedColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 日本の名刺の標準サイズ 91mm x 55mm */
private const val CARD_ASPECT_RATIO = 91f / 55f
