package com.example.businesscard.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.businesscard.data.model.BusinessCard

/** 名刺の見た目(レイアウト固定)。一覧と表示画面で共通に使う。 */
@Composable
fun BusinessCardView(
    card: BusinessCard,
    modifier: Modifier = Modifier,
    // 将来、名刺ごとの背景色・文字色に差し替えるための引数
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Card(
        modifier = modifier.aspectRatio(CARD_ASPECT_RATIO),
        colors = CardDefaults.cardColors(containerColor = backgroundColor, contentColor = textColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                if (card.company.isNotEmpty()) {
                    Text(
                        text = card.company,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (card.title.isNotEmpty()) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = card.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Column {
                if (card.phone.isNotEmpty()) {
                    Text(text = card.phone, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                if (card.email.isNotEmpty()) {
                    Text(
                        text = card.email,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** 日本の名刺の標準サイズ 91mm x 55mm */
private const val CARD_ASPECT_RATIO = 91f / 55f
