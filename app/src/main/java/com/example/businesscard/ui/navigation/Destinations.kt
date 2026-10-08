package com.example.businesscard.ui.navigation

import com.example.businesscard.domain.model.BusinessCard
import kotlinx.serialization.Serializable

/** 型安全ナビゲーションの遷移先。引数はクラスのプロパティとして型付きで渡す。 */

@Serializable
data object CardList

@Serializable
data class CardDetail(val cardId: Long)

/** [cardId] が [BusinessCard.NEW_ID] なら新規作成。 */
@Serializable
data class CardEdit(val cardId: Long = BusinessCard.NEW_ID)
