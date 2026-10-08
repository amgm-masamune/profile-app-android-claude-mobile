package com.example.businesscard.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.ui.glass.rememberGlassFeedback
import com.example.businesscard.ui.theme.SoftGlassShapes
import com.example.businesscard.ui.theme.SoftGlassTheme
import com.example.businesscard.ui.theme.SoftGlassType

/**
 * 名刺の見た目(レイアウト固定)。一覧と表示画面で共通に使う。
 * 見本のボタンと同じ「すりガラスの板 + 下端の光 + 右下の影」を、名刺の大きさにしたもの。
 *
 * @param onClick 指定すると面全体がボタンになる。押すと名刺が指に吸い寄せられて手前へ浮き(影が伸びる)、指の所が光り、
 *   離すとばねで戻る。押す・離すで短く振動する
 */
@Composable
fun BusinessCardView(
    card: BusinessCard,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    // 将来、名刺ごとの背景色・文字色に差し替えるための引数(null = すりガラス)
    backgroundColor: Color? = null,
    textColor: Color = SoftGlassTheme.colors.ink,
) {
    val c = SoftGlassTheme.colors
    val shape = SoftGlassShapes.card
    val mutedColor = textColor.copy(alpha = textColor.alpha * 0.8f)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val feedback = rememberGlassFeedback()

    Column(
        modifier = modifier
            .aspectRatio(CARD_ASPECT_RATIO)
            .glassSurface(
                shape = shape,
                fill = backgroundColor ?: if (pressed) c.glassPressed else c.glass,
                glow = GlassGlow.Strong,
                interactionSource = if (onClick != null) interaction else null,
            )
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
                Text(
                    text = card.company,
                    style = SoftGlassType.cardCompany,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (card.title.isNotEmpty()) {
                Text(
                    text = card.title,
                    style = SoftGlassType.body,
                    color = mutedColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = card.name,
            style = SoftGlassType.cardName,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Column {
            if (card.phone.isNotEmpty()) {
                Text(
                    text = card.phone,
                    style = SoftGlassType.body,
                    color = mutedColor,
                    maxLines = 1,
                )
            }
            if (card.email.isNotEmpty()) {
                Text(
                    text = card.email,
                    style = SoftGlassType.body,
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
