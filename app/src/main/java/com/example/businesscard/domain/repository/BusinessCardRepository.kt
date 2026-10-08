package com.example.businesscard.domain.repository

import com.example.businesscard.domain.model.BusinessCard
import kotlinx.coroutines.flow.Flow

/**
 * 名刺データへのアクセス契約。domain層が定義し、data層が実装する(依存関係逆転)。
 */
interface BusinessCardRepository {
    /** 全名刺(新しい順)。 */
    val cards: Flow<List<BusinessCard>>

    /** 指定した名刺。存在しなければnullを流す。 */
    fun observeCard(id: Long): Flow<BusinessCard?>

    /** [BusinessCard.NEW_ID] なら新規作成、それ以外は更新。 */
    suspend fun save(card: BusinessCard)

    suspend fun delete(id: Long)
}
