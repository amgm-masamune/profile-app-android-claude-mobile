package com.example.businesscard.domain.model

/**
 * 名刺のドメインモデル。Android/Room/Composeのいずれにも依存しない純粋なKotlinクラス。
 * 保存前の名刺は [NEW_ID] を持つ。
 */
data class BusinessCard(
    val id: Long = NEW_ID,
    val name: String,
    val company: String = "",
    val title: String = "",
    val phone: String = "",
    val email: String = "",
) {
    val isNew: Boolean get() = id == NEW_ID

    companion object {
        const val NEW_ID = 0L
    }
}
