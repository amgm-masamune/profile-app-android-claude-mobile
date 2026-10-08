package com.example.businesscard.ui.preview

import com.example.businesscard.domain.model.BusinessCard

/** プレビューとスクリーンショットテストで使う見本データ。 */
internal object SampleCards {
    val taro = BusinessCard(
        id = 1,
        name = "山田 太郎",
        company = "株式会社サンプル",
        title = "エンジニア",
        phone = "090-1234-5678",
        email = "taro@example.com",
    )
    val hanako = BusinessCard(
        id = 2,
        name = "佐藤 花子",
        company = "Example Design Studio",
        title = "デザイナー",
        phone = "080-9876-5432",
        email = "hanako@example.com",
    )
    val all = listOf(taro, hanako)
}
