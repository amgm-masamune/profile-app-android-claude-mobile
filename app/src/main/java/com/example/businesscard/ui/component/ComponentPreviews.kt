package com.example.businesscard.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.businesscard.domain.model.BusinessCard
import com.example.businesscard.ui.theme.BusinessCardTheme
import com.example.businesscard.ui.theme.SoftGlassTheme

/** デザインシステムのカタログ。Android Studio のプレビューで全部品を一覧できる。 */
@Composable
private fun SoftGlassCatalog(darkTheme: Boolean) {
    BusinessCardTheme(darkTheme = darkTheme) {
        SoftGlassBackground {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BusinessCardView(
                    card = BusinessCard(
                        id = 1,
                        name = "山田 太郎",
                        company = "株式会社サンプル",
                        title = "エンジニア",
                        phone = "090-1234-5678",
                        email = "taro@example.com",
                    ),
                )
                GlassTextField(
                    value = "山田 太郎",
                    onValueChange = {},
                    label = "氏名",
                    modifier = Modifier.fillMaxWidth(),
                )
                GlassTextField(
                    value = "",
                    onValueChange = {},
                    label = "会社名",
                    isError = true,
                    errorText = "会社名を入力してください",
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassButton(text = "名刺を追加", onClick = {}, icon = Icons.Default.Add)
                    GlassButton(text = "キャンセル", onClick = {}, style = GlassButtonStyle.Secondary)
                    GlassButton(text = "削除", onClick = {}, style = GlassButtonStyle.Danger)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassIconButton(onClick = {}, icon = Icons.Default.Edit, contentDescription = "編集", primary = true)
                    GlassIconButton(
                        onClick = {},
                        icon = Icons.Default.Delete,
                        contentDescription = "削除",
                        tint = SoftGlassTheme.colors.danger,
                    )
                }
            }
        }
    }
}

@Preview(name = "Soft Glass / Light", showBackground = true, heightDp = 760)
@Composable
private fun SoftGlassCatalogLightPreview() = SoftGlassCatalog(darkTheme = false)

@Preview(name = "Soft Glass / Dark", showBackground = true, heightDp = 760)
@Composable
private fun SoftGlassCatalogDarkPreview() = SoftGlassCatalog(darkTheme = true)
